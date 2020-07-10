<%@ include file="/include.jsp" %>
<%@ page import="com.jetbrains.teamcity.boris.simplePlugin.RemoteTriggerServiceKt" %>
<%@ page import="jetbrains.buildServer.util.StringUtil" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>

<tr class="noBorder">
    <td>
        <em>This trigger starts your build by a schedule</em>
    </td>
</tr>
<tr>
    <td>
        <props:checkboxProperty name="<%=RemoteTriggerServiceKt.ENABLE%>"
                                onclick="window.SimpleTrigger.checkboxUpdate(this);"/>
        <label for="<%=RemoteTriggerServiceKt.ENABLE%>">Enable trigger</label>
        <span class="smallNote">
          consider turning this flag off in case you are tired of this trigger<br/>
        </span>
        <span class="error" id="error_<%=RemoteTriggerServiceKt.ENABLE%>"></span>
    </td>
</tr>
<tr class="delay"
        <% if (!StringUtil.isTrue(propertiesBean.getProperties().get(RemoteTriggerServiceKt.ENABLE))) { %>
    style="display: none"
        <% } %>
>
    <td>
        <label for="<%=RemoteTriggerServiceKt.DELAY%>">Delay, m:</label>
        <props:textProperty name="<%=RemoteTriggerServiceKt.DELAY%>"/>
        <span class="error" id="error_<%=RemoteTriggerServiceKt.DELAY%>"></span>
    </td>
</tr>

<script type="application/javascript">
    window.SimpleTrigger = {
        checkboxUpdate: function (checkbox) {
            if (checkbox.checked) {
                $j('.delay').show();
            } else {
                $j('.delay').hide();
            }
        }
    }
</script>
