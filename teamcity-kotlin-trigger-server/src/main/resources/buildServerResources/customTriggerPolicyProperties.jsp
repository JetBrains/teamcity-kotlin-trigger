<%--@elvariable id="requiredMap" type="java.util.Map<java.lang.String, java.lang.String>"--%>
<%@ include file="/include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.Constants" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggerPropertiesController" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="customTriggersManager" type="jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager"
             scope="request"/>
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>

<tr>
    <td>
        <label for="${Constants.TRIGGER_POLICY_PATH}">Triggering policy:</label>
        <props:selectProperty name="${Constants.TRIGGER_POLICY_PATH}"  onchange="BS.TriggerProperties.getProperties($('${Constants.TRIGGER_POLICY_PATH}').options[$('${Constants.TRIGGER_POLICY_PATH}').selectedIndex].value);">
            <props:option value="">-- Choose a triggering policy --</props:option>
            <c:forEach items="${customTriggersManager.allUsableCustomTriggers(project)}" var="trigger">
                <props:option value="${trigger.filePath}">${trigger.fileName}</props:option>
            </c:forEach>
        </props:selectProperty>
        <span class="error" id="error_<%=Constants.TRIGGER_POLICY_PATH%>"></span>
    </td>
</tr>
<table id="customTriggerParams" class="runnerFormTable"></table>
<tr>
    <td>
        <label for="<%=Constants.ADDITIONAL_PROPERTIES%>">You may specify additional properties for your trigger in format "key=value"</label>
        <props:multilineProperty name="<%=Constants.ADDITIONAL_PROPERTIES%>" linkTitle="Additional properties" cols="35" rows="5"/>
        <span class="error" id="error_<%=Constants.ADDITIONAL_PROPERTIES%>"></span>
    </td>
</tr>
<input type="hidden" name="prop:projectId" value="${project.externalId}">
<input type="hidden" id="requiredMap" name="prop:requiredMap" value="{}">

<script type="application/javascript">
    BS.TriggerProperties = {
        getProperties: function(triggerPolicyPath) {
            if (triggerPolicyPath) {
                let properties = '{}'
                <c:if test="${not empty propertiesBean.properties}">
                    properties = '${util:forJS(CustomTriggerPropertiesController.Companion.serializeMap(propertiesBean.properties), false, false)}'
                </c:if>

                BS.ajaxUpdater('customTriggerParams', window['base_uri'] + '${CustomTriggerPropertiesController.PATH}', {
                    parameters: {
                        triggerPolicyPath,
                        properties
                    },
                    evalScripts: true,
                    onSuccess: function () {
                        window.setTimeout(function () {
                            $j('#requiredMap').val(BS.TriggerProperties.requiredMapStr)
                        }, 100);
                    }
                });
            }
        }
    }
    BS.TriggerProperties.getProperties($('${Constants.TRIGGER_POLICY_PATH}').options[$('${Constants.TRIGGER_POLICY_PATH}').selectedIndex].value);
</script>