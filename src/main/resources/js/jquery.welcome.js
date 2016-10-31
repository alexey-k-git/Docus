var lastResult;
var prevResult;
var lastEvent;
var passive;
var beforelastSelectedDocId;
var lastSelectedDocId;
var сurrentUserRole;
var userName;
var jump;
var selectedNodes;
var nodeSelector;

function enterHowUnregistred() {
	сurrentUserRole = "unregistered";
	userName = "unregistered";
	filterAccessibilityFieldforUser();
	$("#logIn").show();
	$("#logOut").hide();
	$("#welcomeText").html("");
}

function resetVariables() {
	lastResult = "";
	prevResult = "-";
	lastEvent = "";
	passive = false;
	jump = false;
	selectedNodes = [];
}

function resetButtons() {
	document.docusControl.add.disabled = "disabled";
	document.docusControl.del.disabled = "disabled";
	document.docusControl.save.disabled = "disabled";
	document.docusControl.reset.disabled = "disabled";
	document.docusControl.cancel.disabled = "disabled";
	document.docusControl.send.disabled = "disabled";
	document.docusControl.pathfile.disabled = "disabled";
	document.docusControl.getFile.disabled = "disabled";
	document.docusControl.removeFile.disabled = "disabled";
}

function getDocument(path) {
	if (path.length == 0) {
		return;
	} else {
		var treeId = "#" + path.shift();
		if (path.length == 0) {
			$("#tree-div").jstree("deselect_all");
			// $("#tree-div").jstree("select_node", treeId);
			selectedNodes.unshift(treeId);
			nodeSelector = "li" + treeId + " " + "a"
			$(nodeSelector).css("background", "yellow");
			return;

		}
		$("#tree-div").jstree("open_node", treeId, function() {
			getDocument(path);
		});
	}
}

function unique(arr) {
	arr.sort();
	for ( var i = 0; i < arr.length; i++)
		for ( var j = i + 1; j < arr.length;)
			if (arr[i] == arr[j]) {
				arr.splice(j, 1);
			} else {
				j++;
			}
	return arr

}

function addOrChange() {
	if (lastEvent == "Create") {
		setButtonsSaveResetCancel(true, true, true);
	} else {
		jump = false;
		setButtonsSaveResetCancel(true, true, false);
	}
}

function checkChanges() {
	if (prevResult == "-" || jump) {
		jump = false;
		return false;
	}
	var doc = eval('(' + prevResult + ')');
	if (document.docusControl.docName.value != doc.name
			|| document.docusControl.docAuthor.value != doc.author
			|| document.docusControl.docTags.value != doc.tags) {
		return true;
	} else {
		return false;
	}
}

function checkFields() {

	if (document.docusControl.docName.value == ""
			|| document.docusControl.docAuthor.value == ""
			|| document.docusControl.docTags.value == "") {
		return true;
	} else {
		return false;
	}

}

function fillOutForm(docInformation) {
	var arrayParams = docInformation.split("{:-)}");
	filterAccessibilityAddDeleteforUser(true, true);
	document.docusControl.docName.disabled = "";
	document.docusControl.docAuthor.disabled = "";
	document.docusControl.docTags.disabled = "";
	document.docusControl.docName.value = arrayParams[0];
	document.docusControl.docAuthor.value = arrayParams[1];
	document.docusControl.docTags.value = arrayParams[2];
	var flag = false;
	if (arrayParams[3] == "true") {
		flag = true;
	}
	document.docusControl.checkFile.checked = flag;
	document.docusControl.docFileName.value = arrayParams[4];
}

function createResultTable(data) {
	var results = $('#result tbody');
	results.empty();
	if (data.size === 0) {
		results.append($("<tr><td colspan='5'>По вашему запросу ничего не найдено</td></tr>"));
	} else {
		$("#clearButton").show();
		var btnTemplate = $("<button>Перейти</button>");
		$.each(data.items, function(index, value) {
			var btn = btnTemplate.clone().click(function() {
				getDocument(value.path.split(" "));
				return false;
			});
			var rec = $("<tr/>").append($("<td/>").text(value.name)).append(
					$("<td/>").text(value.author)).append(
					$("<td/>").text(value.modified)).append(
					$("<td/>").text(value.file));
			rec.append($("<td/>").append(btn));
			results.append(rec);
		});
	}
}

function login(data) {

	var array = eval('(' + data + ')');
	var result = array.result;

	switch (result) {
	case "noFound":
		alert("Польователь с таким именем не найден.\n            Проверьте правильность ввода.");
		break;
	case "passwordError":
		alert("Данный пароль не соответствует введенному логину.\n            Проверьте правильность ввода.");
		break;
	case "Found": {
		$("#logIn").hide();
		$("#logOut").show();
		$("#welcomeText")
				.append("Welcome to Docus, <b>" + array.name + "</b>!");
		сurrentUserRole = array.role;
		userName = array.name;
	}
		break;
	}
}

function filterAccessibilityFieldforUser() {
	switch (сurrentUserRole) {
	case "unregistered": {
	}
	case "visitor": {
		document.docusControl.docName.disabled = "disabled";
		document.docusControl.docAuthor.disabled = "disabled";
		document.docusControl.docTags.disabled = "disabled";

	}
		break;
	case "admin": {
		document.docusControl.docName.disabled = "";
		document.docusControl.docAuthor.disabled = "";
		document.docusControl.docTags.disabled = "";
	}
		break;
	}
}

function fillOutFormJSON(docInformation) {
	var doc = eval('(' + docInformation + ')');
	filterAccessibilityAddDeleteforUser(true, true);
	filterAccessibilitySendforUser(true);
	filterAccessibilityFieldforUser();
	document.docusControl.docName.value = doc.name;
	document.docusControl.docAuthor.value = doc.author;
	document.docusControl.docTags.value = doc.tags;
	document.docusControl.checkFile.checked = doc.file;
	if (document.docusControl.checkFile.checked) {
		filterAccessibilityDownloadRemoveforUser(true, true);
	} else {
		setButtonsDownloadRemove(false, false);
	}
	document.docusControl.docFileName.value = doc.fileName;
	document.docusControl.pathfile.value = "";
	$("#dateCreated").text(doc.created);
	$("#dateChanged").text(doc.modified);
}

function filterAccessibilityAddDeleteforUser(add, del) {
	switch (сurrentUserRole) {
	case "unregistered": {
	}
	case "visitor": {
		setButtonsAddDelete(false, false);
	}
		break;
	case "admin": {
		setButtonsAddDelete(add, del);
	}
		break;
	}
}

function filterAccessibilitySendforUser(send) {
	switch (сurrentUserRole) {
	case "unregistered": {
	}
	case "visitor": {
		setButtonsSendFile(false);
	}
		break;
	case "admin": {
		setButtonsSendFile(send)
	}
		break;
	}
}

function filterAccessibilityDownloadRemoveforUser(download, remove) {
	switch (сurrentUserRole) {
	case "unregistered": {
		setButtonsDownloadRemove(false, false);
	}
		break;
	case "visitor": {
		setButtonsDownloadRemove(download, false);
	}
		break;
	case "admin": {
		setButtonsDownloadRemove(download, remove);
	}
		break;
	}
}

function setButtonsSendFile(send) {
	$("#linkFile").empty();
	if (send) {
		document.docusControl.send.disabled = "";
		document.docusControl.pathfile.disabled = "";
	} else {
		document.docusControl.send.disabled = "disabled";
		document.docusControl.pathfile.disabled = "disabled";
	}

}

function setButtonsDownloadRemove(download, remove) {
	if (download) {
		document.docusControl.getFile.disabled = "";
	} else {
		document.docusControl.getFile.disabled = "disabled";
	}
	if (remove) {
		document.docusControl.removeFile.disabled = "";
	} else {
		document.docusControl.removeFile.disabled = "disabled";
	}
}

function setButtonsAddDelete(add, del) {

	if (add) {
		document.docusControl.add.disabled = "";
	} else {
		document.docusControl.add.disabled = "disabled";
	}
	if (del) {
		document.docusControl.del.disabled = "";
	} else {
		document.docusControl.del.disabled = "disabled";
	}

}

function setButtonsSaveResetCancel(save, reset, cancel) {
	if (save) {
		document.docusControl.save.disabled = "";
	} else {
		document.docusControl.save.disabled = "disabled";
	}
	if (reset) {
		document.docusControl.reset.disabled = "";
	} else {
		document.docusControl.reset.disabled = "disabled";
	}
	if (cancel) {
		document.docusControl.cancel.disabled = "";
	} else {
		document.docusControl.cancel.disabled = "disabled";
	}
}

function newDoc() {
	document.docusControl.docName.disabled = "";
	document.docusControl.docAuthor.disabled = "";
	document.docusControl.docTags.disabled = "";
	document.docusControl.docName.value = "Имя документа";
	document.docusControl.docAuthor.value = "Автор Документа";
	document.docusControl.docTags.value = "Тэг1, Тэг2";
	var flag = false;
	document.docusControl.checkFile.checked = flag;
	document.docusControl.docFileName.value = "Файл отсутствует";
	$("#dateCreated, #dateChange").text("--.--.--");
	setButtonsAddDelete(false, false)
	setButtonsSaveResetCancel(false, false, true);

}

function rootNode() {
	setButtonsSaveResetCancel(false, false, false);
	filterAccessibilityAddDeleteforUser(true, false);
	setButtonsDownloadRemove(false, false);
	setButtonsSendFile(false);
	document.docusControl.docName.disabled = "disabled";
	document.docusControl.docAuthor.disabled = "disabled";
	document.docusControl.docTags.disabled = "disabled";
	document.docusControl.docName.value = "ROOT";
	document.docusControl.docAuthor.value = "-";
	document.docusControl.docTags.value = "-";
	document.docusControl.checkFile.checked = false;
	document.docusControl.docFileName.value = "-";
	document.docusControl.pathfile.value = "";
	$("#dateCreated, #dateChanged").text("--.--.--");

}

$(function() {

	$("#tree-div")
			.jstree(
					{

						"ui" : {
							"select_limit" : 1
						},

						"plugins" : [ "themes", "json_data", "ui", "crrm",
								"dnd", "types", "search" ],

						"json_data" : {
							"ajax" : {
								"url" : "/childs",
								"data" : function(n) {
									return {
										"operation" : "get_children",
										"id" : n.attr ? n.attr("id").replace(
												"node_", "") : "root"
									};
								}
							}
						},

						"types" : {
							"valid_children" : [ "document" ],
							"types" : {
								"document" : {
									"valid_children" : [ "document" ],
								}
							}
						}
					})
			.bind("loaded.jstree", function(event, data) {

				document.docusControl.docName.disabled = "disabled";
				document.docusControl.docAuthor.disabled = "disabled";
				document.docusControl.docTags.disabled = "disabled";
				rootNode();
				filterAccessibilityAddDeleteforUser(false, false);
				setButtonsSaveResetCancel(false, false, false);

			})
			.bind(
					"select_node.jstree",
					function(event, data) {

						lastSelectedDocId = data.rslt.obj.attr("id");

						$
								.post(
										"/show",
										{
											"id" : data.rslt.obj.attr("id")
										},
										function(r) {
											lastResult = r;
											if (checkFields()) {
												lastEvent = "";
												alert("На этапе редактирования вы оставили пустые поля.\n              Изменения не будут сохранены.      ");
											} else {
												if (lastEvent == "Create") {
													if (confirm("Добавить новый документ?")) {
														lastSelectedDocId = data.rslt.obj
																.attr("id");
														passive = true;
														var parentId = "#";
														parentId += beforelastSelectedDocId;
														var nameDoc = document.docusControl.docName.value;
														$("#tree-div")
																.jstree(
																		"create",
																		parentId,
																		"last",
																		{
																			"data" : nameDoc
																		},
																		false,
																		true);
														return;
													}
												}

												if (checkChanges()) {

													if (confirm("Сохранить 	 изменения в предыдущем	  документе?")) {
														passive = true;
														var docId = "#";
														docId += beforelastSelectedDocId;
														$("#tree-div")
																.jstree(
																		"rename_node",
																		docId,
																		document.docusControl.docName.value);
														return;
													}
												}
											}
											beforelastSelectedDocId = data.rslt.obj
													.attr("id");
											document.getElementById('docId').value = beforelastSelectedDocId;
											if (r == "-") {
												rootNode();

											} else {
												fillOutFormJSON(r);
											}
											prevResult = r;
											setButtonsSaveResetCancel(false,
													false, false);

										});

					}).bind("remove.jstree", function(e, data) {

				data.rslt.obj.each(function() {

					$.ajax({

						async : false,

						type : 'POST',

						url : "/delete",

						data : {

							"id" : this.id.replace("node_", "")

						},

						success : function(r) {
							data.inst.refresh();
						}

					});

				});

			})

			.bind("create.jstree", function(e, data) {
				$.post("/create", {
					"idParent" : beforelastSelectedDocId,
					"name" : document.docusControl.docName.value,
					"author" : document.docusControl.docAuthor.value,
					"tags" : document.docusControl.docTags.value
				}, function(r) {
					lastEvent = "";
					jump = true;
					$(data.rslt.obj).attr("id", r);
					var selectNode = "#";
					if (passive) {
						selectNode += lastSelectedDocId;
						passive = false;
						beforelastSelectedDocId = lastSelectedDocId;
						$("#tree-div").jstree("select_node", selectNode);
						if (lastResult == "-") {
							rootNode()
						} else {
							fillOutFormJSON(lastResult);
						}

					} else {
						selectNode += r;
					}
					$("#tree-div").jstree("deselect_all");
					$("#tree-div").jstree("select_node", selectNode);
				});
			}

			).bind("rename_node.jstree", function(e, data) {

				$.post(

				"/change",

				{

					"id" : data.rslt.obj.attr("id"),
					"name" : document.docusControl.docName.value,
					"author" : document.docusControl.docAuthor.value,
					"tags" : document.docusControl.docTags.value

				},

				function(r) {
					jump = true;
					if (passive) {
						var selectNode = "#";
						selectNode += lastSelectedDocId;
						passive = false;
						$("#tree-div").jstree("select_node", selectNode);
						beforelastSelectedDocId = lastSelectedDocId;
						setButtonsSaveResetCancel(false, false, false);
						fillOutFormJSON(lastResult);
					}
				});

			}).bind("move_node.jstree", function(event, data) {
				alert(data.rslt.p);
				$.post("/move", {
					"selectedId" : data.rslt.o.attr("id"),
					"destinationId" : data.rslt.r.attr("id"),
					"option" : data.rslt.p

				}, function(r) {
					alert(r);
				});

			});

	$("#deleteDoc").click(function() {
		$("#tree-div").jstree("remove");
	});

	$("#findButton").click(function() {
		if (сurrentUserRole === "unregistered") {
			return;
		}
		var queryText = document.searchForm.queryField.value;
		var queryType = document.searchForm.query.value;
		$.post("/find", {
			"queryType" : queryType,
			"queryText" : queryText
		}).done(function(data) {
			createResultTable(data);
		});

	});

	$("#clearButton").click(function() {

		selectedNodes = unique(selectedNodes);
		for ( var i = 0; i < selectedNodes.length; i++) {
			nodeSelector = "li" + selectedNodes[i] + " " + "a"
			$(nodeSelector).css("background", "");
		}
		selectedNodes = [];
	});

	$("#sendfile").click(function() {
		var path = $('#filepath').val();
		var currentId = $('#tree-div').jstree('get_selected').attr('id');
		$("#docId").val(currentId);
		$("#formm").submit();
	});

	$("#loginButtonIn").click(function() {
		$.post("/login", {
			"login" : document.loginForm.name.value,
			"password" : document.loginForm.password.value
		}).done(function(data) {
			login(data);

		});
	});

	$("#loginButtonOut").click(function() {
		resetButtons();
		resetVariables();
		enterHowUnregistred();
	});

	$("#downloadFile").click(
			function() {
				var currentId = $('#tree-div').jstree('get_selected')
						.attr('id');
				$("#linkFile").empty();
				var form = $("#downloadForm");
				form.find("[name=id]").val(currentId);
				form.submit();
			});

	$("#createDoc").click(function() {
		lastEvent = "Create";
		newDoc();
	});

	$("#saveDoc").click(function() {
		if (checkFields()) {
			alert("Заполните, пожалуйста, пустые поля.");
			return;
		}
		var nameDoc = document.docusControl.docName.value;
		if (lastEvent == "Create") {
			$("#tree-div").jstree("create", null, "last", {
				"data" : nameDoc
			}, false, true);
		} else {
			$("#tree-div").jstree("rename_node", null, nameDoc);
		}
		filterAccessibilityAddDeleteforUser(true, true);
		setButtonsSaveResetCancel(false, false, false);

	});

	$("#cancelDoc").click(function() {

		if (lastEvent == "Create") {
			if (lastResult != "-") {
				fillOutFormJSON(lastResult);
				filterAccessibilityAddDeleteforUser(true, true);
				setButtonsSaveResetCancel(false, false, false);
			} else {
				rootNode();
			}
			lastEvent = "";
		}
	});

	$("#resetDoc").click(function() {
		if (lastEvent == "Create") {
			newDoc();
		} else {
			fillOutFormJSON(lastResult);
			filterAccessibilityAddDeleteforUser(true, true);
			setButtonsSaveResetCancel(false, false, false);
		}
	});

	$("#deleteFile").click(function() {
		$('#linkFile').empty();
		var currentId = $('#tree-div').jstree('get_selected').attr('id');
		$.post("/deletefile", {
			"id" : currentId
		}).done(function(data) {
			alert("\"" + data + "\"" + " was removed");
			setButtonsDownloadRemove(false, false);
			document.docusControl.checkFile.checked = false;
			document.docusControl.docFileName.value = "-";

		});

	});

	$('#nameDoc').bind('textchange', function(event, previousText) {
		addOrChange();
	});
	$('#authorDoc').bind('textchange', function(event, previousText) {
		addOrChange();
	});
	$('#tagsDoc').bind('textchange', function(event, previousText) {
		addOrChange();
	});

	$(document).ready(function() {
		enterHowUnregistred();
		resetVariables();
		$("#clearButton").hide();
		$("#formm").ajaxForm(function(data) {
			document.docusControl.pathfile.value = "";
			setButtonsDownloadRemove(true, true);
			var fileInfo = eval('(' + data + ')');
			document.docusControl.checkFile.checked = fileInfo.file;
			document.docusControl.docFileName.value = fileInfo.fileName;
			$("#dateChanged").text(fileInfo.modified);
		});

	});
});