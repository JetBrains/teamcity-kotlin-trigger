<%@ include file="/include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.Constants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="remoteTriggersBean" type="jetbrains.buildServer.buildTriggers.remote.controller.RemoteTriggersBean"
             scope="request"/>

<tr class="noBorder">
    <td>
        <em>Choose one trigger from the list below</em>
    </td>
</tr>
<tr>
    <td>
        <label for="<%=Constants.TRIGGER_POLICY%>">Triggering policy:</label>
        <props:selectProperty name="<%=Constants.TRIGGER_POLICY%>">
            <props:option value="">-- Choose a triggering policy --</props:option>
            <c:forEach items="${remoteTriggersBean.fileNames}" var="fileName">
                <props:option value="${remoteTriggersBean.getFullPathTo(fileName)}">${fileName}</props:option>
            </c:forEach>
        </props:selectProperty>
        <span class="error" id="error_<%=Constants.TRIGGER_POLICY%>"></span>
    </td>
</tr>
<tr>
    <td>
        <label for="<%=Constants.PROPERTIES%>">Specify properties for your trigger in format "key=value"</label>
        <props:multilineProperty name="<%=Constants.PROPERTIES%>" linkTitle="Properties" cols="35" rows="5"/>
        <span class="error" id="error_<%=Constants.PROPERTIES%>"></span>
    </td>
</tr>