<%--@elvariable id="requiredMap" type="java.util.Map<java.lang.String, java.lang.String>"--%>
<%@ include file="/include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.TriggerUtil" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggerPropertiesController" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="customTriggersManager" type="jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager"
             scope="request"/>
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>

<tr>
    <td>
        <label for="${TriggerUtil.TRIGGER_POLICY_NAME}">Triggering policy:</label>
        <props:selectProperty name="${TriggerUtil.TRIGGER_POLICY_NAME}" onchange="BS.TriggerProperties.getProperties($('${TriggerUtil.TRIGGER_POLICY_NAME}').options[$('${TriggerUtil.TRIGGER_POLICY_NAME}').selectedIndex].value);">
            <props:option value="">-- Choose a triggering policy --</props:option>
            <c:forEach items="${customTriggersManager.allUsableCustomTriggers(project)}" var="trigger">
                <props:option value="${trigger.policyName}">${trigger.policyName}</props:option>
            </c:forEach>
        </props:selectProperty>
        <span class="error" id="error_<%=TriggerUtil.TRIGGER_POLICY_NAME%>"></span>
    </td>
</tr>
<table id="customTriggerParams" class="runnerFormTable"></table>
<input type="hidden" name="prop:projectId" value="${project.externalId}">
<input type="hidden" id="requiredMap" name="prop:requiredMap" value="{}">

<script type="application/javascript">
    BS.TriggerProperties = {
        getProperties: function(triggerPolicyName) {
            if (triggerPolicyName) {
                let properties = '{}'
                <c:if test="${not empty propertiesBean.properties}">
                    properties = '${util:forJS(CustomTriggerPropertiesController.Companion.serializeMap(propertiesBean.properties), false, false)}'
                </c:if>

                BS.ajaxUpdater('customTriggerParams', window['base_uri'] + '${CustomTriggerPropertiesController.PATH}', {
                    parameters: {
                        triggerPolicyName,
                        properties,
                        projectId: '${project.externalId}'
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
    BS.TriggerProperties.getProperties($('${TriggerUtil.TRIGGER_POLICY_NAME}').options[$('${TriggerUtil.TRIGGER_POLICY_NAME}').selectedIndex].value);
</script>