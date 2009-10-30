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

eXo.webui.UITabbedDashboard = {
	
	init : function(){},
	
	renameTabLabel : function(e){
		if(!e){
			e = window.event;
		}
		var keyNum = e.keyCode;
		
		//If user presses on ENTER button, then rename the tab label
		if(keyNum == 13){
			var inputElement = eXo.core.Browser.getEventSource(e);
			var newTabLabel = inputElement.value;
			if(newTabLabel.length < 1){
				return;
			}
			var DOMUtil = eXo.core.DOMUtil;
			var portletFrag = DOMUtil.findAncestorByClass(inputElement, "PORTLET-FRAGMENT");
			var compId = portletFrag.parentNode.id;
			var nodeIndex = inputElement.id;
			
			//Change the tab label
			var spanElement = document.createElement("span");
			spanElement.innerHTML = newTabLabel;
			inputElement.parentNode.replaceChild(spanElement, inputElement);
			
			//Send request to server to change node name
			var href = eXo.env.server.portalBaseURL + "?portal:componentId=" + compId;
			href += "&portal:type=action";
			href += "&portal:isSecure=false";
			href += "&uicomponent=UITabPaneDashboard";
			href += "&op=RenameTabLabel";
			href += "&objectId=" + nodeIndex;
			href += "&newTabLabel=" + encodeURIComponent(newTabLabel);
			window.location = href;
			return;			
		}
		//If user presses on the ESCAPE key, then reset the span element on the tab
		else if(keyNum == 27){
			var inputElement = eXo.core.Browser.getEventSource(e);
//			var spanElement = document.createElement("span");
//			spanElement.ondblclick = "#"; //TODO: Set the callback on this property
//			spanElement.innerHTML = inputElement.name;
//			inputElement.parentNode.replaceChild(spanElement, inputElement);
			if(eXo.webui.UITabbedDashboard.backupElement) {
 				inputElement.parentNode.replaceChild(eXo.webui.UITabbedDashboard.backupElement, inputElement);
 				eXo.webui.UITabbedDashboard.backupElement = null;
			}
		}
	},

	showEditLabelInput : function(selectedElement, nodeIndex, currentContent){
		eXo.webui.UITabbedDashboard.backupElement = selectedElement;
		var prNode = selectedElement.parentNode;
		var tabContainer = eXo.core.DOMUtil.findAncestorByClass(prNode, "TabsContainer");
		var addButton = eXo.core.DOMUtil.findFirstChildByClass(tabContainer, "div", "AddDashboard");
		
		var inputElement = document.createElement("input");
		inputElement.type = "text";
		inputElement.id = nodeIndex;
		inputElement.name = currentContent; // To store old value
		inputElement.value = currentContent;
		inputElement.style.border = "1px solid #b7b7b7";
		inputElement.style.width = "95px";
		inputElement.onkeypress = eXo.webui.UITabbedDashboard.renameTabLabel;
		inputElement.setAttribute('maxLength', 50);
		inputElement.onblur = function() {
			prNode.replaceChild(eXo.webui.UITabbedDashboard.backupElement, inputElement);
		};
		
		prNode.replaceChild(inputElement, selectedElement);
		inputElement.focus();
	},
	
	createDashboardPage : function(e){
		if(!e){
			e = window.event;
		}	
		var keyNum = e.keyCode;
		
		//If user presses on ENTER button
		if(keyNum == 13){
			var inputElement = eXo.core.Browser.getEventSource(e);
			var newTabLabel = inputElement.value;
			
			//Send request to server to change node name
			var href = eXo.env.server.portalBaseURL + "?portal:componentId=" + inputElement.id;
			href += "&portal:type=action";
			href += "&portal:isSecure=false";
			href += "&uicomponent=UITabPaneDashboard";
			href += "&op=AddDashboard";
			href += "&objectId=" + newTabLabel;
			window.location = href;
		}
		//If user presses on ESCAPE button
		else if(keyNum == 27){
			var inputElement = eXo.core.Browser.getEventSource(e);
			var editingTabElement = eXo.core.DOMUtil.findAncestorByClass(inputElement, "UITab GrayTabStyle");
			
			//Remove the editing tab
			editingTabElement.parentNode.removeChild(editingTabElement);
		}
	},
	
	cancelTabDashboard : function(e){
		if(!e){
			e = window.event;
		}
		var inputElement = eXo.core.Browser.getEventSource(e);
		var editingTabElement = eXo.core.DOMUtil.findAncestorByClass(inputElement, "UITab GrayTabStyle");
		
		//Remove the editing tab
		editingTabElement.parentNode.removeChild(editingTabElement);
	},
	
	createTabDashboard : function(addTabElement){
		var DOMUtil = eXo.core.DOMUtil;
		var tabContainer = addTabElement.parentNode;
		var tabElements = DOMUtil.findChildrenByClass(tabContainer, "div", "UITab GrayTabStyle");
		var portletFrag = DOMUtil.findAncestorByClass(tabContainer, "PORTLET-FRAGMENT");
		var selectedTabElement = DOMUtil.findFirstDescendantByClass(tabContainer, "div", "SelectedTab");
		
		var newTabElement = selectedTabElement.parentNode.cloneNode(true);
		tabContainer.insertBefore(newTabElement, addTabElement);
		
		var inputElement = document.createElement("input");
		inputElement.type = "text";
		inputElement.value = "Tab_" + tabElements.length;
		inputElement.style.border = "1px solid #b7b7b7";
		inputElement.style.width = "95px";
		inputElement.onkeypress = eXo.webui.UITabbedDashboard.createDashboardPage;
		inputElement.onblur = eXo.webui.UITabbedDashboard.cancelTabDashboard;
		inputElement.id = portletFrag.parentNode.id; //Store the id of the portlet here
		
		var spanElement = DOMUtil.findDescendantsByTagName(newTabElement, "span")[0];
		spanElement.parentNode.replaceChild(inputElement, spanElement);
		
		DOMUtil.findNextElementByTagName(inputElement, "a").href = "#";
		inputElement.focus();		
	}
}