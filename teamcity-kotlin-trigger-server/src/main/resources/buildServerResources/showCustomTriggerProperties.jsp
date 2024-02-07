<%@ include file="/include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggerPropertiesController" %>


<jsp:useBean id="parameters" type="java.util.Map<java.lang.String, jetbrains.buildServer.serverSide.ControlDescription>"
             scope="request"/>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="requiredMap" type="java.util.Map<java.lang.String, java.lang.String>" scope="request"/>

<c:if test="${not empty parameters}">
    <tr class="groupingTitle">
        <td colspan="2">Trigger properties</td>
    </tr>
</c:if>
<c:forEach var="parameter" items="${parameters}">
    <tr>
        <td>
            <label for="${parameter.key}">
                    ${util:forJS(parameter.value.parameterTypeArguments["description"], false, true)}:<c:if test="${parameter.value.parameterTypeArguments['required'] == 'true'}"><l:star/></c:if>
            </label>
        </td>
        <td>
            <c:choose>
                <c:when test="${parameter.value.parameterType == 'boolean'}">
                    <props:checkboxProperty name="${parameter.key}"/>
                </c:when>

                <c:otherwise>
                    <props:textProperty name="${parameter.key}"/>
                </c:otherwise>
            </c:choose>

            <span class="error" id="error_${parameter.key}"></span>
        </td>
    </tr>
</c:forEach>

<script>
    BS.TriggerProperties.requiredMapStr = '{}'
    <c:if test="${not empty requiredMap}">
        BS.TriggerProperties.requiredMapStr = '${util:forJS(CustomTriggerPropertiesController.Companion.serializeMap(requiredMap), false, false)}'
    </c:if>
</script>