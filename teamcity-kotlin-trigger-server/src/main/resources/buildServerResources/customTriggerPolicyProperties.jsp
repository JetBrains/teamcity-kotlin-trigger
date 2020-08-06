<%@ include file="/include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.Constants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggerPropertiesController" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="customTriggersManager" type="jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager"
             scope="request"/>
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>

<tr>
    <td>
        <label for="${Constants.TRIGGER_POLICY_PATH}">Triggering policy:</label>
        <props:selectProperty name="${Constants.TRIGGER_POLICY_PATH}" onchange="BS.TriggerProperties.getProperties($('${Constants.TRIGGER_POLICY_PATH}').options[$('${Constants.TRIGGER_POLICY_PATH}').selectedIndex].value);">
            <props:option value="">-- Choose a triggering policy --</props:option>
            <c:forEach items="${customTriggersManager.allUsableCustomTriggerFiles(project)}" var="trigger">
                <props:option value="${trigger.filePath}">${trigger.fileName}</props:option>
            </c:forEach>
        </props:selectProperty>
        <span class="error" id="error_<%=Constants.TRIGGER_POLICY_PATH%>"></span>
    </td>
</tr>
<tr>
    <td id="customTriggerParams"></td>
</tr>
<tr>
    <td>
        <label for="<%=Constants.PROPERTIES%>">Specify properties for your trigger in format "key=value"</label>
        <props:multilineProperty name="<%=Constants.PROPERTIES%>" linkTitle="Properties" cols="35" rows="5"/>
        <span class="error" id="error_<%=Constants.PROPERTIES%>"></span>
    </td>
</tr>

<script type="application/javascript">
    BS.TriggerProperties = {
        getProperties: function(triggerPolicyPath) {
            if (triggerPolicyPath !== '') {
                BS.ajaxUpdater('customTriggerParams', window['base_uri'] + '${CustomTriggerPropertiesController.PATH}', {
                    parameters: {
                        triggerPolicyPath
                    },
                    // parameters: 'triggerName=' + encodeURIComponent(triggerName) + "&triggerId=" + this.formElement().triggerId.value + "&id=" + buildTypeId,
                    evalScripts: true,
                    onSuccess: function () {
                        window.setTimeout(function () {
                            BS.AvailableParams.attachPopups('settingsId=' + buildTypeId, 'textProperty', 'multilineProperty');
                            that.recenterDialog();
                        }, 100);
                    }
                });
            }
        }
    }
</script>