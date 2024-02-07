<%@ include file="/include-internal.jsp" %>

<%@ include file="_uploadTriggerDialog.jspf" %>
<%@ include file="_customTriggerPolicyActions.jspf" %>
<%@ include file="_assignAccessTokenDialog.jspf" %>



<jsp:useBean id="customTriggersManager" type="jetbrains.buildServer.buildTriggers.remote.CustomTriggersManager"
             scope="request"/>
<jsp:useBean id="permissionChecker" type="jetbrains.buildServer.buildTriggers.remote.PermissionChecker" scope="request"/>
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>

<bs:linkCSS>${teamcityPluginResourcesPath}_customTriggerPoliciesTab.css</bs:linkCSS>

<c:set var="localTriggers" value="${customTriggersManager.localCustomTriggers(project)}"/>
<c:set var="inheritedTriggers" value="${customTriggersManager.inheritedCustomTriggers(project)}"/>
<c:set var="canEditProject" value="${permissionChecker.canEditProject(project)}"/>

<table class="parametersTable section noMargin" style="border: none">
    <forms:addButton onclick="return BS.UploadTriggerDialog.open();">Upload trigger policy jar</forms:addButton>
    <c:if test="${not empty localTriggers}">
        <thead>
        <tr>
            <td colspan="5" style="border: none">
                <h2 class="triggerTitle">Local trigger policies</h2>
            </td>
        </tr>
        </thead>

        <thead>
        <tr>
            <th style="width: 35%">Trigger Policy Name</th>
            <th colspan="2">Usages in this project and its subprojects</th>
        </tr>
        </thead>
        <c:forEach var="localTrigger" items="${localTriggers}">
            <c:set var="enabled" value="${customTriggersManager.isTriggerPolicyEnabled(localTrigger)}"/>
            <tr class="triggerRow" style="${not enabled ? 'color: #999': ''}">
                <c:set var="usages" value="${customTriggersManager.getUsages(localTrigger)}"/>
                <c:set var="hasToken"
                       value="${not empty customTriggersManager.getTriggerPolicyAuthToken(localTrigger)}"/>
                <td>
                    <c:out value="${localTrigger.policyName}"/>
                    <c:if test="${not enabled}">
                        <span class="inheritedParam">(disabled)</span>
                    </c:if>
                    <c:if test="${hasToken}">
                        <span class="inheritedParam"
                              onmouseover="BS.Tooltip.showMessageAtCursor(event, {shift:{x:5,y:10}}, 'This policy has an access token assigned');"
                              onmouseout="BS.Tooltip.hidePopup();">(authorized)</span>
                    </c:if>
                </td>
                <td colspan="${canEditProject ? 1 : 2}">
                    <c:if test="${empty usages}">
                        No usages
                    </c:if>
                    <c:forEach var="usage" items="${usages}">
                        <c:out value="${usage.fullName}"/><br>
                    </c:forEach>
                </td>
                <c:if test="${canEditProject}">
                    <td class="edit" style="border: 1px solid #ccc">
                        <c:set var="canEditSubprojects" value="${permissionChecker.canEditBuildTypeIdentities(usages)}"/>
                        <bs:actionsPopup controlId="pA_local_${localTrigger.policyName}"
                                         popup_options="shift: {x: -150, y: 20}, className: 'quickLinksMenuPopup'">
                            <jsp:attribute name="content">
                              <div>
                                  <ul class="menuList">
                                      <c:if test="${canEditSubprojects}">
                                          <l:li>
                                            <a href="#"
                                               onclick="return BS.TriggerPolicy.update(String.raw`${localTrigger.policyName}`)"
                                               title="Update trigger policy">Update...</a>
                                          </l:li>
                                          <c:choose>
                                          <c:when test="${enabled}">
                                              <l:li>
                                                  <a href="#"
                                                     onclick="return BS.TriggerPolicy.setEnabled(String.raw`${localTrigger.policyName}`, false, ${not empty usages})"
                                                     title="Disable all triggers of this policy">Disable...</a>
                                              </l:li>
                                          </c:when>
                                          <c:otherwise>
                                              <l:li>
                                                  <a href="#"
                                                     onclick="return BS.TriggerPolicy.setEnabled(String.raw`${localTrigger.policyName}`, true, ${not empty usages})"
                                                     title="Enable all triggers of this policy">Enable...</a>
                                              </l:li>
                                          </c:otherwise>
                                          </c:choose>
                                          <c:if test="${empty usages}">
                                              <l:li>
                                                  <a href="#"
                                                     onclick="return BS.TriggerPolicy.delete(String.raw`${localTrigger.policyName}`)"
                                                     title="Delete this policy">Delete...</a>
                                              </l:li>
                                          </c:if>
                                          <c:choose>
                                          <c:when test="${hasToken}">
                                              <l:li>
                                                  <a href="#"
                                                     onclick="return BS.TriggerPolicy.removeToken(String.raw`${localTrigger.policyName}`)"
                                                     title="Remove the assigned access token from this policy">Remove access token...</a>
                                              </l:li>
                                          </c:when>
                                          <c:otherwise>
                                              <l:li>
                                                  <a href="#"
                                                     onclick="return BS.TriggerPolicy.assignToken(String.raw`${localTrigger.policyName}`)"
                                                     title="Assign an access token to this policy">Assign an access token...</a>
                                              </l:li>
                                          </c:otherwise>
                                          </c:choose>
                                      </c:if>
                                  </ul>
                              </div>
                            </jsp:attribute>
                            <jsp:body></jsp:body>
                        </bs:actionsPopup>
                    </td>
                </c:if>
            </tr>
        </c:forEach>
    </c:if>
    <c:if test="${not empty inheritedTriggers}">
        <thead style="margin-top: auto">
        <tr>
            <td colspan="6" style="border: none">
                <h2 class="triggerTitle">Inherited trigger policies</h2>
            </td>
        </tr>
        </thead>

        <thead>
        <tr>
            <th style="width: 35%">Trigger Policy Name</th>
            <th>Usages in this project and its subprojects</th>
            <th>Project</th>
        </tr>
        </thead>
        <c:forEach var="inheritedTrigger" items="${inheritedTriggers}">
            <c:set var="enabled" value="${customTriggersManager.isTriggerPolicyEnabled(inheritedTrigger)}"/>
            <tr class="triggerRow" style="${not enabled ? 'color: #999': ''}">
                <td>
                    <c:set var="hasToken"
                           value="${not empty customTriggersManager.getTriggerPolicyAuthToken(inheritedTrigger)}"/>
                    <c:out value="${inheritedTrigger.policyName}"/>
                    <c:if test="${not enabled}">
                        <span class="inheritedParam">(disabled)</span>
                    </c:if>
                    <c:if test="${hasToken}">
                        <span class="inheritedParam"
                              onmouseover="BS.Tooltip.showMessageAtCursor(event, {shift:{x:5,y:10}}, 'This policy has an access token assigned');"
                              onmouseout="BS.Tooltip.hidePopup();">(authorized)</span>
                    </c:if>
                </td>
                <td>
                    <c:set var="usages" value="${customTriggersManager.getUsages(inheritedTrigger)}"/>
                    <c:if test="${empty usages}">
                        No usages
                    </c:if>
                    <c:forEach var="usage" items="${usages}">
                        <c:out value="${usage.fullName}"/><br>
                    </c:forEach>
                </td>
                <td>
                    <c:out value="${inheritedTrigger.project.fullName}"/>
                </td>
            </tr>
        </c:forEach>
    </c:if>
</table>