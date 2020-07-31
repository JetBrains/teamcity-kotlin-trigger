<%--<%@ include file="/include.jsp" %>--%>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="afn" uri="/WEB-INF/functions/authz" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<jsp:useBean id="customTriggersBean" type="jetbrains.buildServer.buildTriggers.remote.controller.CustomTriggersBean"
             scope="request"/>

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

<c:if test="${not empty customTriggersBean}">
    <c:set var="localTriggers" value="${customTriggersBean.localFileNames}"/>
    <c:set var="inheritedTriggers" value="${customTriggersBean.parentsFileNames}"/>
    <c:set var="canEditProject"
           value="${afn:permissionGrantedForProjectWithId(customTriggersBean.projectId, \"EDIT_PROJECT\")}"/>
</c:if>

<table class="parametersTable section noMargin" style="border: none">
    <div id="uploadTriggerButton">
        <forms:addButton onclick="return BS.UploadTriggerDialog.open();">Upload trigger jar</forms:addButton>
        <div class="shift"></div>
    </div>
    <c:if test="${not empty localTriggers}">
        <thead>
        <tr>
            <td colspan="5" style="border: none">
                <h2 class="triggerTitle">Local triggers</h2>
            </td>
        </tr>
        </thead>

        <thead>
        <tr>
            <th style="width: 35%">Trigger Name</th>
            <c:choose>
                <c:when test="${canEditProject}">
                    <th colspan="2">Usages</th>
                </c:when>
                <c:otherwise>
                    <th>Usages</th>
                </c:otherwise>
            </c:choose>
        </tr>
        </thead>
        <c:forEach var="localTrigger" items="${localTriggers}">
            <tr class="triggerRow">
                <td>
                    <c:out value="${localTrigger}"/>
                </td>
                <td colspan="${canEditProject ? 1 : 2}">
                    No usages
                </td>
                <c:if test="${canEditProject}">
                    <td class="edit" style="border: 1px solid #ccc">
<%--                        <a href="#"--%>
<%--                           onclick="return BS.Plugins.toggleEnabledStatus('${util:forJS(plugin.pluginName, true, true)}', '${util:forJS(plugin.name, true, true)}', '--%>
<%--                               <bs:escapeForJs--%>
<%--                                       text="${plugin.UUID}"/>', ${plugin.reloadable ? plugin.notLoaded : plugin.disabled}, ${plugin.reloadable})"--%>
<%--                           triggerTitle="<c:out value="${actionName}"/> plugin">Delete</a>--%>
                        Cannot delete
                    </td>
                </c:if>
            </tr>
        </c:forEach>
    </c:if>
    <c:if test="${not empty inheritedTriggers}">
        <thead style="margin-top: auto">
        <tr>
            <td colspan="6" style="border: none">
                <h2 class="triggerTitle">Inherited triggers</h2>
            </td>
        </tr>
        </thead>

        <thead>
        <tr>
            <th style="width: 35%">Trigger Name</th>
            <th colspan="2">Usages in this project and its subprojects</th>
        </tr>
        </thead>
        <c:forEach var="inheritedTrigger" items="${inheritedTriggers}">
            <tr class="triggerRow">
                <td>
                    <c:out value="${inheritedTrigger}"/>
                </td>
                <td colspan="2">
                    No usages
                </td>
            </tr>
        </c:forEach>
    </c:if>
</table>

<bs:dialog dialogId="uploadTriggerDialog" title="Upload trigger jar" closeCommand="BS.UploadTriggerDialog.close()"
           dialogClass="uploadDialog">
    <forms:multipartForm id="uploadTriggerForm" action="uploadCustomTrigger.html" targetIframe="hidden-iframe"
                         onsubmit="return BS.UploadTriggerDialog.validate();">
        <input type="text" id="fileName" name="fileName" value="" class="mediumField" style="display:none;"/>
        <input type="text" id="projectId" name="projectId" value="${customTriggersBean.projectId}"
               style="display: none"/>
        <table class="runnerFormTable">
            <tr>
                <th>Trigger</th>
                <td>
                    <forms:file name="fileToUpload"/>
                    <div id="uploadError" class="error hidden"></div>
                </td>
            </tr>
        </table>
        <div class="popupSaveButtonsBlock">
            <forms:submit id="uploadTriggerDialogSubmit" label="Upload trigger jar"/>
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

        open: function () {
            this.show();
            return false;
        }
    })))

    BS.UploadTriggerDialog.prepareFileUpload();
</script>
