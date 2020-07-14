<%@ include file="/include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.Constants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>

<tr class="noBorder">
    <td>
        <em>This trigger starts your build by a schedule</em>
    </td>
</tr>
<tr>
    <td>
        <props:checkboxProperty name="<%=Constants.Request.ENABLE%>"
                                onclick="window.SimpleTrigger.checkboxUpdate(this);"/>
        <label for="<%=Constants.Request.ENABLE%>">Enable trigger</label>
        <span class="smallNote">
          consider turning this flag off in case you are tired of this trigger<br/>
        </span>
        <span class="error" id="error_<%=Constants.Request.ENABLE%>"></span>
    </td>
</tr>
<tr id="delay">
    <td>
        <label for="<%=Constants.Request.DELAY%>">Delay, m:</label>
        <props:textProperty name="<%=Constants.Request.DELAY%>"/>
        <span class="error" id="error_<%=Constants.Request.DELAY%>"></span>
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
    if (!$j('#<%=Constants.Request.ENABLE%>').checked) {
        $j('#delay').hide();
    }
</script>
