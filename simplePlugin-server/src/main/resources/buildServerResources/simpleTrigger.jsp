<%@ include file="/include.jsp" %>
<%@ page import="com.jetbrains.teamcity.boris.simplePlugin.SimpleTrigger" %>
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
        <props:checkboxProperty name="<%=SimpleTrigger.ENABLE_PROPERTY%>"
                                onclick="window.SimpleTrigger.checkboxUpdate(this);"/>
        <label for="<%=SimpleTrigger.ENABLE_PROPERTY%>">Enable trigger</label>
        <span class="smallNote">
          consider turning this flag off in case you are tired of this trigger<br/>
        </span>
        <span class="error" id="error_<%=SimpleTrigger.ENABLE_PROPERTY%>"></span>
    </td>
</tr>
<tr class="delay"
        <% if (!StringUtil.isTrue(propertiesBean.getProperties().get(SimpleTrigger.ENABLE_PROPERTY))) { %>
    style="display: none"
        <% } %>
>
    <td>
        <label for="<%=SimpleTrigger.DELAY_PROPERTY%>">Delay, m:</label>
        <props:textProperty name="<%=SimpleTrigger.DELAY_PROPERTY%>"/>
        <span class="error" id="error_<%=SimpleTrigger.DELAY_PROPERTY%>"></span>
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
