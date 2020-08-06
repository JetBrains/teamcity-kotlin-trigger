<%@ include file="/include.jsp" %>
<jsp:useBean id="parameters" type="java.util.Map<java.lang.String, jetbrains.buildServer.serverSide.ControlDescription>"
             scope="request"/>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<c:forEach var="parameter" items="${parameters}">
    <tr>
        <th>
                ${util:forJS(parameter.value.parameterTypeArguments["description"], false, true)}:<c:if test="${parameter.value.parameterTypeArguments['required'] == 'true'}"><l:star/></c:if>
        </th>
        <td>
            <c:choose>
                <c:when test="${parameter.value.parameterType == 'password'}">
                    <props:passwordProperty name="${parameter.key}"/>
                </c:when>
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