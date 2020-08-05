<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<%--@elvariable id="error" type="java.lang.String"--%>
<%--@elvariable id="jsBase" type="java.lang.String"--%>
<script type="text/javascript">
    <c:choose>
        <c:when test="${not empty error}">
            parent.${jsBase}.error("${util:forJS(error, true, false)}");
        </c:when>
        <c:otherwise>
            parent.${jsBase}.closeAndRefresh();
        </c:otherwise>
    </c:choose>
</script>
