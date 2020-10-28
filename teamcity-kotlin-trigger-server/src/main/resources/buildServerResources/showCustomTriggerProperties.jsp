<%@ include file="/include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggerPropertiesController" %>
<%--
  ~ Copyright 2000-2020 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ https://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

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