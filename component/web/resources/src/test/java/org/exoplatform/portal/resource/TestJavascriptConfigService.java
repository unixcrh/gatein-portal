/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.portal.resource;

import javax.servlet.ServletContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.component.test.web.WebAppImpl;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.test.mocks.servlet.MockServletContext;
import org.exoplatform.test.mocks.servlet.MockServletRequest;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.application.javascript.JavascriptConfigParser;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.URIWriter;
import org.gatein.common.io.IOTools;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.script.FetchMode;
import org.gatein.portal.controller.resource.script.ScriptResource;
import org.json.JSONObject;

/**
 * @author <a href="mailto:phuong.vu@exoplatform.com">Vu Viet Phuong</a>
 * @version $Id$
 */
public class TestJavascriptConfigService extends AbstractWebResourceTest
{
   private static final ControllerContext CONTROLLER_CONTEXT = new MockControllerContext();

   private JavascriptConfigService jsService;

   private static ServletContext mockServletContext;
   
   private static boolean isFirstStartup = true;   
   
   @Override
   protected void setUp() throws Exception
   {
      final PortalContainer portalContainer = getContainer();
      jsService = (JavascriptConfigService)portalContainer.getComponentInstanceOfType(JavascriptConfigService.class);
      
      if (isFirstStartup)
      {
         Map<String, String> resources = new HashMap<String, String>(6);
         resources.put("/js/script1.js", "aaa;");
         resources.put("/js/script2.js", "bbb;");
         resources.put("/js/module1.js", "ccc;");
         resources.put("/js/module2.js", "ddd;");
         resources.put("/js/native1.js", "eee;");
         resources.put("/js/native2.js", "fff;");
         resources.put("/js/common.js", "kkk;");
         resources.put("/js/normalize_test.js", " \n /* // */  //  /* \n  /* /*  */  \n  ggg; // /* */ \n");
         mockServletContext = new MockJSServletContext("mockwebapp", resources);
         jsService.registerContext(new WebAppImpl(mockServletContext, Thread.currentThread().getContextClassLoader()));

         URL url = portalContainer.getPortalClassLoader().getResource("mockwebapp/gatein-resources.xml");
         JavascriptConfigParser.processConfigResource(url.openStream(), jsService, mockServletContext);

         isFirstStartup = false;
      }     
   }
   
   public void testGetScript() throws Exception
   {      
      //no wrapper for SCRIPTS
      String script1 = "aaa;" +
                               "if (typeof define === 'function' && define.amd && !require.specified('SHARED/script1')) {define('SHARED/script1');}";
      String script2 = "bbb;" +
                               "if (typeof define === 'function' && define.amd && !require.specified('SHARED/script2')) {define('SHARED/script2');}";
      assertReader(script1, jsService.getScript(new ResourceId(ResourceScope.SHARED, "script1"), null));
      assertReader(script2, jsService.getScript(new ResourceId(ResourceScope.SHARED, "script2"), null));          
      
      //wrapper for MODULE
      //test for Alias : module1 is used with 'm1' alias
      String module1 = "define('SHARED/module1', [], function() {" +
                                 "var require = eXo.require,requirejs = require,define = eXo.define;define.names=[];define.deps=[];" +      
                                 "return ccc;});";
      assertReader(module1, jsService.getScript(new ResourceId(ResourceScope.SHARED, "module1"), null));
      
      //module2 depends on module1
      //test for Alias : module1 is used with 'mod1' alias, module2 use default name for alias
      String module2 = "define('SHARED/module2', [\"SHARED/module1\"], function(mod1) {" +
                                 "var require = eXo.require,requirejs = require,define = eXo.define;define.names=[\"mod1\"];define.deps=[mod1];" +
                                 "return ddd;});";
      assertReader(module2, jsService.getScript(new ResourceId(ResourceScope.SHARED, "module2"), null));
   }
     
   public void testCommonJS() throws Exception
   {
      String commonjs = "define('SHARED/commonjs', [\"require\",\"exports\",\"module\"], function(require,exports,module) {" +
               "var require = eXo.require,requirejs = require,define = eXo.define;define.names=[\"require\",\"exports\",\"module\"];" +
               "define.deps=[eXo.require,exports,module];" +      
               "return kkk;});";
      assertReader(commonjs, jsService.getScript(new ResourceId(ResourceScope.SHARED, "commonjs"), null));
   }
   
   public void testGroupingScript() throws Exception
   {
      String module1 = "define('SHARED/module1', [], function() {" +      
               "var require = eXo.require,requirejs = require,define = eXo.define;define.names=[];define.deps=[];" +
               "return ccc;});";
      String module2 = "define('SHARED/module2', [\"SHARED/module1\"], function(mod1) {" +
               "var require = eXo.require,requirejs = require,define = eXo.define;define.names=[\"mod1\"];define.deps=[mod1];" +
               "return ddd;});";
      
      StringWriter buffer = new StringWriter();
      IOTools.copy(jsService.getScript(new ResourceId(ResourceScope.GROUP, "fooGroup"), null), buffer);
      
      //the order of module1, and module2 in group is not known
      String result = buffer.toString();
      result = result.replace(module1, "");
      result = result.replace(module2, "");
      assertTrue(result.isEmpty());
   }
   
   public void testGetNativeScript() throws Exception
   {
      String native1 = "eee;";
      assertReader(native1, jsService.getScript(new ResourceId(ResourceScope.SHARED, "native1"), null));
      
      String native2 = "fff;" +
               "if (typeof define === 'function' && define.amd && !require.specified('native2')) {define('native2');}";
      assertReader(native2, jsService.getScript(new ResourceId(ResourceScope.SHARED, "native2"), null));
   }

   public void testGetJSConfig() throws Exception
   {            
      JSONObject config = jsService.getJSConfig(CONTROLLER_CONTEXT, null);

      //All SCRIPTS and remote resource have to had dependencies declared in shim configuration
      JSONObject shim = config.getJSONObject("shim");
      assertNotNull(shim);
      
      JSONObject remoteDep = shim.getJSONObject("remote2");
      assertNotNull(remoteDep);
      assertEquals("{\"deps\":[\"remote1\"]}", remoteDep.toString());
      
      JSONObject scriptDep = shim.getJSONObject("SHARED/script2");
      assertNotNull(scriptDep);
      assertEquals("{\"deps\":[\"SHARED/script1\"]}", scriptDep.toString());
      
      //requireJS need a map of all resourceIDs and urls
      //requireJS add ".js" automatically so we truncate it from url
      JSONObject paths = config.getJSONObject("paths");
      assertNotNull(paths);
      //Return remote module/script url as it's  declared in gatein-resources.xml
      assertEquals("/js/remote1", paths.getString("remote1"));
      assertEquals("/js/remote2", paths.getString("remote2"));
      
      //Return native module/script url as it's  declared in gatein-resources.xml
      assertEquals("mock_url_of_native1", paths.getString("native1"));
      assertEquals("mock_url_of_native2", paths.getString("native2"));
      
      //module1 and module2 are grouped
      assertEquals("mock_url_of_fooGroup", paths.getString("SHARED/module1"));
      assertEquals("mock_url_of_fooGroup", paths.getString("SHARED/module2"));

      //navController url for scripts
      assertEquals("mock_url_of_script1", paths.getString("SHARED/script1"));
      assertEquals("mock_url_of_script2", paths.getString("SHARED/script2"));
   }
   
   public void testGenerateURL() throws Exception
   {
      ResourceId remote1 = new ResourceId(ResourceScope.SHARED, "remote1");
      String remoteURL = jsService.generateURL(CONTROLLER_CONTEXT, remote1, false, false, null);
      //Return remote module/script url as it's  declared in gatein-resources.xml
      assertEquals("/js/remote1.js", remoteURL);
      
      ResourceId native1 = new ResourceId(ResourceScope.SHARED, "native1");      
      remoteURL = jsService.generateURL(CONTROLLER_CONTEXT, native1, false, false, null);
      assertEquals("mock_url_of_native1.js", remoteURL);
      
      ResourceId module1 = new ResourceId(ResourceScope.SHARED, "module1");      
      remoteURL = jsService.generateURL(CONTROLLER_CONTEXT, module1, false, false, null);
      assertEquals("mock_url_of_module1.js", remoteURL);
   }
   
   public void testNormalize() throws Exception
   {
      String normalizedJS = "define('SHARED/normalize_test', [], function() {" +
                                          "var require = eXo.require,requirejs = require,define = eXo.define;define.names=[];define.deps=[];" +
                                          "return ggg; // /* */ \n});";
      assertReader(normalizedJS, jsService.getScript(new ResourceId(ResourceScope.SHARED, "normalize_test"), null));
   }
   
   public void testResolveIDs()
   {
      Map<ResourceId, FetchMode> tmp = new HashMap<ResourceId, FetchMode>();
      tmp.put(new ResourceId(ResourceScope.SHARED, "script2"), null);
      
      Map<ScriptResource, FetchMode> ids = jsService.resolveIds(tmp);
      //Only resolve dependencies for SCRIPTS
      assertEquals(2, ids.size());
      List<ScriptResource> result = new ArrayList<ScriptResource>(ids.keySet());
      assertEquals("script1", result.get(0).getId().getName());
      assertEquals("script2", result.get(1).getId().getName());
      
      tmp.clear();      
      //module2 depends on module1 but we don't resolve dependencies for it
      tmp.put(new ResourceId(ResourceScope.SHARED, "module2"), null);
      ids = jsService.resolveIds(tmp);
      assertEquals(1, ids.size());
      assertEquals("module2", ids.keySet().iterator().next().getId().getName());
   }
   
/*
   public void testCommonJScripts()
   {
      assertEquals(5, jsService.getCommonJScripts().size());
      assertTrue(jsService.isModuleLoaded("js.test1"));
      assertTrue(jsService.isModuleLoaded("js.test2"));
      assertTrue(jsService.isModuleLoaded("js.test3"));
      assertTrue(jsService.isModuleLoaded("js.test4"));
      assertTrue(jsService.isModuleLoaded("js.test7"));
      
      assertFalse(jsService.isModuleLoaded("js.test5"));
      
      //
      InputStream script = jsService.getScript(new ResourceId(ResourceScope.SHARED, "common"), "js.test1");
      assertNotNull(script);
   }
*/

   private void assertReader(String expected, Reader actual) throws Exception
   {
      StringWriter buffer = new StringWriter();
      IOTools.copy(actual, buffer, 1);
      assertEquals(expected, buffer.toString());
   }

/*
   public void testPortalJScript() throws IOException
   {
      Collection<Javascript> site = jsService.getPortalJScripts("site1");
      assertEquals(1, site.size());
      Iterator<Javascript> iterator = site.iterator();
      assertEquals(mockServletContext.getContextPath() + "/js/test5.js", iterator.next().getPath());

      //
      InputStream script = jsService.getScript(new ResourceId(ResourceScope.PORTAL, "site1"));
      assertNotNull(script);
      assertEquals("", read(script));

      site = jsService.getPortalJScripts("site2");
      assertEquals(2, site.size());
      iterator = site.iterator();
      assertEquals(mockServletContext.getContextPath() + "/js/test6.js", iterator.next().getPath());
      assertEquals(mockServletContext.getContextPath() + "/js/test5.js", iterator.next().getPath());

      //
      script = jsService.getScript(new ResourceId(ResourceScope.PORTAL, "site2"));
      assertNotNull(script);
      assertEquals("", read(script));

      //
      assertNull(jsService.getPortalJScripts("classic"));
      assertNull(jsService.getScript(new ResourceId(ResourceScope.PORTAL, "classic")));

      //
      jsService.removePortalJScripts("site1");
      assertNull(jsService.getPortalJScripts("site1"));
      assertNull(jsService.getScript(new ResourceId(ResourceScope.PORTAL, "site1")));

      //
      Javascript portalJScript = Javascript.create(new ResourceId(ResourceScope.PORTAL, "/portal"), "portal1", "/mockwebapp", "/portal", Integer.MAX_VALUE);
      jsService.addPortalJScript(portalJScript);
      String jScript = jsService.getJScript(portalJScript.getPath());
      assertNull(jScript);
      assertEquals("", read(script));
      resResolver.addResource(portalJScript.getPath(), "bar1");
      jScript = jsService.getJScript(portalJScript.getPath());
      assertEquals("bar1", jScript);
   }
*/

   private static class MockControllerContext extends ControllerContext
   {
      public MockControllerContext()
      {
         super(null, null, new MockServletRequest(null, null), null, null);
      }

      @Override
      public void renderURL(Map<QualifiedName, String> parameters, URIWriter uriWriter) throws IOException
      {
         uriWriter.appendSegment("mock_url_of_" + parameters.get(QualifiedName.create("gtn", "resource")) + ".js");
      }      
   }
   
   private static class MockJSServletContext extends MockServletContext
   {
      private Map<String, String> resources;
      
      public MockJSServletContext(String contextName, Map<String, String> resources)
      {
         super(contextName);
         this.resources = resources;
      }
      
      public String getContextPath()
      {
         return "/" + getServletContextName();
      }
      
      @Override
      public InputStream getResourceAsStream(String s)
      {
         String input = resources.get(s);
         if (input != null)
         {
            return new ByteArrayInputStream(input.getBytes());
         }
         else
         {
            return null;
         }
      }
   }
}
