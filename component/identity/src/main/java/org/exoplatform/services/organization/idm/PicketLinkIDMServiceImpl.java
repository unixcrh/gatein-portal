/**
 * Copyright (C) 2009 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.organization.idm;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.database.HibernateService;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.IdentitySessionFactory;
import org.picketlink.idm.api.cfg.IdentityConfiguration;
import org.picketlink.idm.cache.APICacheProvider;
import org.picketlink.idm.common.exception.IdentityConfigurationException;
import org.picketlink.idm.impl.cache.JBossCacheAPICacheProviderImpl;
import org.picketlink.idm.impl.configuration.IdentityConfigurationImpl;
import org.picketlink.idm.impl.configuration.jaxb2.JAXB2IdentityConfiguration;
import org.picketlink.idm.spi.configuration.metadata.IdentityConfigurationMetaData;
import org.picocontainer.Startable;

import java.io.InputStream;
import java.net.URL;

import javax.naming.InitialContext;

/*
 * @author <a href="mailto:boleslaw.dawidowicz at redhat.com">Boleslaw Dawidowicz</a>
 */
public class PicketLinkIDMServiceImpl implements PicketLinkIDMService, Startable
{

   private static Log log_ = ExoLogger.getLogger(PicketLinkIDMServiceImpl.class);

   public static final String PARAM_CONFIG_OPTION = "config";

   public static final String PARAM_HIBERNATE_PROPS = "hibernate.properties";

   public static final String PARAM_HIBERNATE_MAPPINGS = "hibernate.mappings";

   public static final String PARAM_HIBERNATE_ANNOTATIONS = "hibernate.annotations";

   public static final String PARAM_JNDI_NAME_OPTION = "jndiName";

   public static final String REALM_NAME_OPTION = "portalRealm";

   public static final String CACHE_CONFIG_OPTION = "cacheConfig";

   private IdentitySessionFactory identitySessionFactory;

   private String config;

   private String realmName = "idm_realm";

   private String cacheConfig;

   private IdentityConfiguration identityConfiguration;

   private PicketLinkIDMServiceImpl()
   {
   }

   public PicketLinkIDMServiceImpl(
      InitParams initParams,
      HibernateService hibernateService,
      ConfigurationManager confManager,
      IdentityCacheService identityCache,
      InitialContextInitializer dependency) throws Exception
   {
      ValueParam config = initParams.getValueParam(PARAM_CONFIG_OPTION);
      ValueParam jndiName = initParams.getValueParam(PARAM_JNDI_NAME_OPTION);
      ValueParam realmName = initParams.getValueParam(REALM_NAME_OPTION);
      ValueParam cacheConfig = initParams.getValueParam(CACHE_CONFIG_OPTION);

      if (config == null && jndiName == null)
      {
         throw new IllegalStateException("Either '" + PARAM_CONFIG_OPTION + "' or '" + PARAM_JNDI_NAME_OPTION
            + "' parameter must " + "be specified");
      }
      if (realmName != null)
      {
         this.realmName = realmName.getValue();
      }

      if (config != null)
      {
         this.config = config.getValue();
         URL configURL = confManager.getURL(this.config);

         if (configURL == null)
         {
            throw new IllegalStateException("Cannot fine resource: " + this.config);
         }



         IdentityConfigurationMetaData configMD =
            JAXB2IdentityConfiguration.createConfigurationMetaData(confManager.getInputStream(this.config));

         identityConfiguration = new IdentityConfigurationImpl().configure(configMD);

         identityConfiguration.getIdentityConfigurationRegistry().register(hibernateService.getSessionFactory(), "hibernateSessionFactory");

         if (cacheConfig != null)
         {
            InputStream configStream = confManager.getInputStream(cacheConfig.getValue());
            JBossCacheAPICacheProviderImpl cacheProvider = new JBossCacheAPICacheProviderImpl();
            cacheProvider.initialize(configStream);
            identityCache.register(cacheProvider);
            identityConfiguration.getIdentityConfigurationRegistry().register(cacheProvider, "apiCacheProvider");
         }
      }
      else
      {
         identitySessionFactory = (IdentitySessionFactory)new InitialContext().lookup(jndiName.getValue());
      }

   }

   public void start()
   {
      if (identitySessionFactory == null)
      {
         try
         {
            identitySessionFactory = identityConfiguration.buildIdentitySessionFactory();
         }
         catch (IdentityConfigurationException e)
         {
            throw new RuntimeException(e);
         }
      }
   }

   public void stop()
   {
   }
                                    
   public IdentitySessionFactory getIdentitySessionFactory()
   {
      return identitySessionFactory; //To change body of implemented methods use File | Settings | File Templates.
   }

   public IdentitySession getIdentitySession() throws Exception
   {
      return getIdentitySessionFactory().getCurrentIdentitySession(realmName);
   }

   public IdentitySession getIdentitySession(String realm) throws Exception
   {
      if (realm == null)
      {
         throw new IllegalArgumentException("Realm name cannot be null");
      }
      return getIdentitySessionFactory().getCurrentIdentitySession(realm);
   }
}
