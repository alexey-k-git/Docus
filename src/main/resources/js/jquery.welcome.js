// общие переменные
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

/*
Здесь список функций предназначенных для управления элементами страницы
*/

// функция для входа как незарегистрированный пользователь
function enterHowUnregistred() {
	сurrentUserRole = "unregistered";
	userName = "unregistered";
	filterAccessibilityFieldforUser();
	$("#logIn").show();
	$("#logOut").hide();
	$("#welcomeText").html("");
}

// функция для сброса переменных
function resetVariables() {
	lastResult = "";
	prevResult = "-";
	lastEvent = "";
	passive = false;
	jump = false;
	selectedNodes = [];
}

// функция для сброса доступности кнопок управления
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

// рекурсивная функция которая находит и открывает нужный нам узел дерева зная путь к нему в виде id
function getDocument(path) {
	if (path.length == 0) {
		return;
	} else {
		var treeId = "#" + path.shift();
		if (path.length == 0) {
			$("#tree-div").jstree("deselect_all");
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

// функция определяет какие кнопки делать активными в зависимости от текущей команды пользователя (создать элемент или изменить текущий)
function addOrChange() {
	if (lastEvent == "Create") {
		setButtonsSaveResetCancel(true, true, true);
	} else {
		jump = false;
		setButtonsSaveResetCancel(true, true, false);
	}
}

// функция проверки изменений в текущем документе
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

// проверка полей документов на заполненость
function checkFields() {

	if (document.docusControl.docName.value == ""
			|| document.docusControl.docAuthor.value == ""
			|| document.docusControl.docTags.value == "") {
		return true;
	} else {
		return false;
	}

}

// устаревшая функция заполнения формы документа
function fillOutForm(docInformation) {
    /*
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
	*/
}

// функция для формирования таблицы с результатом поиска
function createResultTable(data) {
var results = $("table#resulttable tbody");
var array = eval('(' + data + ')');
results.empty();
	if (array.size <1) {
    	$("#result").show();
		results.append($("<tr><td colspan='5'>По вашему запросу ничего не найдено</td></tr>"));
	} else {
	    $("#result").show();
		$("#clearButton").show();
		var btnTemplate = $("<button>Перейти</button>");
		$.each(array.items, function(index, value) {
			var btn = btnTemplate.clone().click(function() {
				getDocument(value.path.split("-"));
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

// функция проверки текущего пользователя в базе данных (если вход выполнен верно наделяет его всеми доступными правами)
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
				.append("Добро пожаловать, <b>" + array.name + "</b>!");
		сurrentUserRole = array.role;
		userName = array.name;
	}
		break;
	}
}

// функция для фильтра доступности полей ввода в зависимости от роли пользователя
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

// функция для заполнения формы документа используя объект JSON
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

// функция для фильтра доступности кнопок удалить и добавить в зависимости от роли пользователя
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

// функция для фильтра доступности кнопки отправить файл в зависимости от роли пользователя
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

// функция для фильтра доступности кнопки удалить и загузить файл в зависимости от роли пользователя ?
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

// функция для фильтра доступности кнопки отправить файл
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

// функция для фильтра доступности кнопок получить и удалить файл
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

// функция для установки доступности кнопок добавить и удалить документ
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

// функция для установки доступности кнопок удалить, сбросить, отмена
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

// функция для заполнения полей вновь создаваемого документа
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

// функция для заполнения полей корневого элемента дерева документов
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


/*
Далее идут обработчики событий для каждого элемента на странице
*/

// JQUERY
$(function() {
    // работа с элементами дерева непосредственно в дереве
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
                // загрузка дерева
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
                        // нажатие на элемент дерева
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
				$.post("/move", {
					"selectedId" : data.rslt.o.attr("id"),
					"destinationId" : data.rslt.r.attr("id"),
					"option" : data.rslt.p

				}, function(r) {
					alert(r);
				});

			});

    // обработчик события нажатия кнопки Удалить
	$("#deleteDoc").click(function() {
		$("#tree-div").jstree("remove");
	});

    // обработчик события нажатия кнопки Найти
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

    // обработчик события нажатия кнопки Очистить
	$("#clearButton").click(function() {

		selectedNodes = unique(selectedNodes);
		for ( var i = 0; i < selectedNodes.length; i++) {
			nodeSelector = "li" + selectedNodes[i] + " " + "a"
			$(nodeSelector).css("background", "");
		}
		selectedNodes = [];
	});

    // обработчик события нажатия кнопки Отправить файл
	$("#sendfile").click(function() {
		var path = $('#filepath').val();
		var currentId = $('#tree-div').jstree('get_selected').attr('id');
		$("#docId").val(currentId);
		$("#formm").submit();
	});

    // обработчик события нажатия кнопки Войти
	$("#loginButtonIn").click(function() {
		$.post("/login", {
			"login" : document.loginForm.name.value,
			"password" : document.loginForm.password.value
		}).done(function(data) {
			login(data);

		});
	});

    // обработчик события нажатия кнопки Выйти
	$("#loginButtonOut").click(function() {
		resetButtons();
		resetVariables();
		enterHowUnregistred();
	});

    // обработчик события нажатия кнопки Получить файл
	$("#downloadFile").click(
			function() {
				var currentId = $('#tree-div').jstree('get_selected')
						.attr('id');
				$("#linkFile").empty();
				var form = $("#downloadForm");
				form.find("[name=id]").val(currentId);
				form.submit();
			});

    // обработчик события нажатия кнопки Создать
	$("#createDoc").click(function() {
		lastEvent = "Create";
		newDoc();
	});

    // обработчик события нажатия кнопки Сохранить
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

    // обработчик события нажатия кнопки Отмена
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

    // обработчик события нажатия кнопки Сбросить изменения
	$("#resetDoc").click(function() {
		if (lastEvent == "Create") {
			newDoc();
		} else {
			fillOutFormJSON(lastResult);
			filterAccessibilityAddDeleteforUser(true, true);
			setButtonsSaveResetCancel(false, false, false);
		}
	});

    // обработчик события нажатия кнопки Удалить файл
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

    // обработчик события изменения текста в поле ввода Имя документа
	$('#nameDoc').bind('textchange', function(event, previousText) {
		addOrChange();
	});

	// обработчик события изменения текста в поле ввода Автор
	$('#authorDoc').bind('textchange', function(event, previousText) {
		addOrChange();
	});

	// обработчик события изменения текста в поле ввода Тэги
	$('#tagsDoc').bind('textchange', function(event, previousText) {
		addOrChange();
	});

    // обработчик события готовности страницы
	$(document).ready(function() {
		enterHowUnregistred();
		resetVariables();
		$("#clearButton").hide();
		$("#result").hide();
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