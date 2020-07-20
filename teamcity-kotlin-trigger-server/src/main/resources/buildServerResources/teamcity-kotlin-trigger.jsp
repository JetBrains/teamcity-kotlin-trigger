<%@ include file="/include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.Constants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="remoteTriggersBean" type="jetbrains.buildServer.buildTriggers.remote.controller.RemoteTriggersBean"
             scope="request"/>

<tr class="noBorder">
    <td>
        <em>This trigger starts your build by a schedule</em>
    </td>
</tr>
<tr>
    <td>
        <props:checkboxProperty name="<%=Constants.ENABLE%>"
                                onclick="window.SimpleTrigger.checkboxUpdate(this);"/>
        <label for="<%=Constants.ENABLE%>">Enable trigger</label>
        <span class="smallNote">
          consider turning this flag off in case you are tired of this trigger<br/>
        </span>
        <span class="error" id="error_<%=Constants.ENABLE%>"></span>
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
<tr id="delay">
    <td>
        <label for="<%=Constants.DELAY%>">Delay, m:</label>
        <props:textProperty name="<%=Constants.DELAY%>"/>
        <span class="error" id="error_<%=Constants.DELAY%>"></span>
    </td>
</tr>

<script type="application/javascript">
    window.SimpleTrigger = {
        checkboxUpdate: function (checkbox) {
            if (checkbox.checked) {
                $j('#delay').show();
            } else {
                $j('#delay').hide();
            }
        }
    }
    if (!$j('#<%=Constants.ENABLE%>').checked) {
        $j('#delay').hide();
    }
</script>
