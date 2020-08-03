<%@ include file="/include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.Constants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="customTriggersManager" type="jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggersManager"
             scope="request"/>
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>

<tr>
    <td>
        <label for="<%=Constants.TRIGGER_POLICY_PATH%>">Triggering policy:</label>
        <props:selectProperty name="<%=Constants.TRIGGER_POLICY_PATH%>">
            <props:option value="">-- Choose a triggering policy --</props:option>
            <c:forEach items="${customTriggersManager.allUsableCustomTriggerFiles(project)}" var="trigger">
                <props:option value="${trigger.filePath}">${trigger.fileName}</props:option>
            </c:forEach>
        </props:selectProperty>
        <span class="error" id="error_<%=Constants.TRIGGER_POLICY_PATH%>"></span>
    </td>
</tr>
<tr>
    <td>
        <label for="<%=Constants.PROPERTIES%>">Specify properties for your trigger in format "key=value"</label>
        <props:multilineProperty name="<%=Constants.PROPERTIES%>" linkTitle="Properties" cols="35" rows="5"/>
        <span class="error" id="error_<%=Constants.PROPERTIES%>"></span>
    </td>
</tr>