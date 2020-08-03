<%--<%@ include file="/include.jsp" %>--%>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="afn" uri="/WEB-INF/functions/authz" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<jsp:useBean id="customTriggersManager" type="jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggersManager"
             scope="request"/>
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>

<style>
    .triggerRow td {
        padding: 0.6em 1em;
        border: 1px solid #ccc;
    }

    .triggerTitle {
        margin-left: -0.8em;
        margin-top: 1em;
        margin-bottom: 0.2em;
        border: none;
    }
</style>

<c:if test="${not empty customTriggersManager}">
    <c:set var="localTriggers" value="${customTriggersManager.localCustomTriggers(project)}"/>
    <c:set var="inheritedTriggers" value="${customTriggersManager.inheritedCustomTriggerFiles(project)}"/>
    <c:set var="canEditProject" value="${customTriggersManager.canEditProject(project)}"/>
</c:if>

<table class="parametersTable section noMargin" style="border: none">
    <div id="uploadTriggerButton">
        <forms:addButton onclick="return BS.UploadTriggerDialog.open();">Upload trigger policy jar</forms:addButton>
        <div class="shift"></div>
    </div>
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
            <tr class="triggerRow">
                <c:set var="usageProjects" value="${localTrigger.getUsagesInProjectAndSubprojects(project)}"/>
                <td>
                    <c:out value="${localTrigger.fileName}"/>
                </td>
                <td colspan="${canEditProject ? 1 : 2}">
                    <c:if test="${empty usageProjects}">
                        No usages
                    </c:if>
                    <c:forEach var="usageProject" items="${usageProjects}">
                        <c:out value="${usageProject.fullName}"/><br>
                    </c:forEach>
                </td>
                <c:if test="${canEditProject}">
                    <td class="edit" style="border: 1px solid #ccc">
                        <c:set var="canEditSubprojects" value="${customTriggersManager.canEditProjects(usageProjects)}"/>
                        <bs:actionsPopup controlId="pA_local_${localTrigger.fileName}"
                                         popup_options="shift: {x: -150, y: 20}, className: 'quickLinksMenuPopup'">
                            <jsp:attribute name="content">
                              <div>
                                  <ul class="menuList">
                                      <l:li>
                                        <a href="#"
                                           onclick="return BS.TriggerPolicy.update('${localTrigger.fileName}')"
                                           title="Update trigger policy">Update...</a>
                                      </l:li>
                                      <c:if test="${canEditSubprojects}">
                                          <l:li>
                                              <a href="#"
                                                 onclick="return BS.TriggerPolicy.disable()"
                                                 title="Disable all triggers of this policy">Disable...</a>
                                          </l:li>
                                          <l:li>
                                              <a href="#"
                                                 onclick="return BS.TriggerPolicy.delete()"
                                                 title="Delete this policy and all its triggers">Delete...</a>
                                          </l:li>
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
            <tr class="triggerRow">
                <td>
                    <c:out value="${inheritedTrigger.fileName}"/>
                </td>
                <td>
                    <c:set var="usageProjects" value="${inheritedTrigger.getUsagesInProjectAndSubprojects(project)}"/>
                    <c:if test="${empty usageProjects}">
                        No usages
                    </c:if>
                    <c:forEach var="usageProject" items="${usageProjects}">
                        <c:out value="${usageProject.fullName}"/><br>
                    </c:forEach>
                </td>
                <td>
                    <c:out value="${inheritedTrigger.project.fullName}"/>
                </td>
            </tr>
        </c:forEach>
    </c:if>
</table>

<bs:dialog dialogId="uploadTriggerDialog" title="Upload trigger policy jar" closeCommand="BS.UploadTriggerDialog.close()"
           dialogClass="uploadDialog">
    <forms:multipartForm id="uploadTriggerForm" action="uploadCustomTriggerPolicy.html" targetIframe="hidden-iframe"
                         onsubmit="return BS.UploadTriggerDialog.validate();">
        <input type="text" id="fileName" name="fileName" value="" class="mediumField" style="display:none;"/>
        <input type="text" id="projectId" name="projectId" value="${project.projectId}"
               style="display: none"/>
        <input type="text" id="updatedFileName" name="updatedFileName" value="" style="display: none"/>
        <table class="runnerFormTable">
            <tr>
                <th>Trigger Policy</th>
                <td>
                    <forms:file name="fileToUpload"/>
                    <div id="uploadError" class="error hidden"></div>
                </td>
            </tr>
        </table>
        <div class="popupSaveButtonsBlock">
            <forms:submit id="uploadTriggerDialogSubmit" label="Upload trigger policy jar"/>
            <forms:cancel onclick="BS.UploadTriggerDialog.close()"/>
            <forms:saving id="uploadingProgress" savingTitle="Uploading..."/>
        </div>
    </forms:multipartForm>
</bs:dialog>

<script type="text/javascript">
    BS.UploadTriggerDialog = OO.extend(BS.AbstractWebForm, OO.extend(BS.AbstractModalDialog, OO.extend(BS.FileBrowse, {
        onFileChanged: function (filename) {
        },

        getContainer: function () {
            return $('uploadTriggerDialog');
        },

        formElement: function () {
            return $('uploadTriggerForm');
        },

        refresh: function () {
            BS.reload(true);
        },

        savingIndicator: function () {
            return $j('#uploadingProgress');
        },

        open: function (updatedFile) {
            $j('#updatedFileName').val(updatedFile)
            this.show();
            return false;
        }
    })))

    BS.UploadTriggerDialog.prepareFileUpload();

    BS.TriggerPolicy = {
        update: function (updatedFileName) {
            return BS.UploadTriggerDialog.open(updatedFileName);
        },

        disable: function () {
            BS.confirmDialog.show({
                text: "Disable this triggering policy and all its triggers?",
                actionButtonText: 'Disable',
                cancelButtonText: 'Cancel',
                title: 'Disable triggering policy',
                action: function () {
                    var completed = $j.Deferred();
                    BS.ajaxRequest(window["base_uri"] + "/admin/plugins.html", {
                        parameters: {uuid: uuid, reload: true, action: "setEnabled"},
                        onComplete: function (transport) {
                            completed.resolve();
                            BS.reload(true);
                        }
                    });
                    return completed;
                }
            });
        },

        delete: function () {
            BS.confirmDialog.show({
                text: 'Delete this triggering policy and all its triggers?',
                actionButtonText: 'Delete',
                cancelButtonText: 'Cancel',
                title: 'Delete triggering policy',
                action: function () {
                    var completed = $j.Deferred();
                    BS.ajaxRequest(window["base_uri"] + "/admin/plugins.html", {
                        parameters: {uuid: uuid, reload: true, action: "setEnabled"},
                        onComplete: function (transport) {
                            completed.resolve();
                            BS.reload(true);
                        }
                    });
                    return completed;
                }
            });
        }
    }
</script>
