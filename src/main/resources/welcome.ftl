<!DOCTYPE html>
<HTML xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>DOCUS web</title>
	<link rel="stylesheet" type="text/css" href="/css/main.css">
	<link rel="stylesheet" type="text/css" href="/css/style.css">
    <script type="text/javascript" src="/js/jquery.js"></script>
    <script type="text/javascript" src="/js/jquery.form.js"></script>
    <script type="text/javascript" src="/js/jquery.jstree.js"></script>
    <script type="text/javascript" src="/js/jquery.textchange.js"></script>
    <script type="text/javascript" src="/js/jquery.welcome.js"></script>
  </head>
  <body>
   <center>
  <table border='1px'>
  <tr>
  <td valign="top">
  
  <form name="loginForm" method="post" action="/login">
  	<h1>Authorization</h1>
  	<br/>
  	<br/>
  	<div id="logIn">
	Логин: <br/>
	<input name="name" type="text" size="20" maxlength="30" value="" /> <br/>
	Пароль: <br/>
	<input name="password" type="password" size="20" maxlength="30" value="" /> <br/>
	<input type="button" id="loginButtonIn" name="enter" value="Вход" />
	</div>
	<span id="welcomeText"></span>
	<div id="logOut">
	<input type="button" id="loginButtonOut" name="out" value="Выход" />
	</div>
	
</form>
  </td>
  <td>
  <center>
    <h1>DOCUS</h1>
    <br>
    <h2>Главная форма управления документами</h2>
    <br>
    <form  name="docusControl" method="post" action="upload" id="formm" enctype="multipart/form-data">
    <table id="mainpanel" border="1">
	<tr><td id="treetd" rowspan="13"><div id="tree-div"><ul></ul></div></td><td colspan="3">Описание</td></tr>
	<tr><td colspan="3">Имя:</td></tr>
	<tr><td colspan="3"><input type="text" id="nameDoc" name="docName"></td></tr>
	<tr><td colspan="3">Автор:</td></tr>
	<tr><td colspan="3"><input type="text" id="authorDoc" name="docAuthor"></td></tr>
	<tr><td colspan="3">Теги:</td></tr>
	<tr><td colspan="3"><textarea rows="3" cols="20" id="tagsDoc" name="docTags"></textarea></td></tr>
	<tr><td colspan="2">Наличие файла:</td><td><input type="checkbox" name="checkFile" disabled="disabled"></td></tr>
	<tr><td colspan="3">Имя файла:</td></tr>
	<tr><td colspan="3"><input style="text-align:center" type="text" name="docFileName" disabled="disabled"></td></tr>
	<tr><td colspan="2">Дата создания:</td><td><span id="dateCreated">--.--.--</span></td></tr>
	<tr><td colspan="2">Последнее изменение:</td><td><span id="dateChanged">--.--.--</span></td></tr>
	<tr><td>
	<input type="hidden" name="id" id="docId" value="0">
   	<input name="pathfile" type="file" id="filepath"><br>
   	<input type="button" name="send" value="Отправить" id="sendfile">
	</td>
	<td><input type="button" name="getFile" id="downloadFile" value="Получить файл"><br><span id="linkFile"></span></td>
	<td><input type="button" name="removeFile" id="deleteFile" value="Удалить файл"></td>
	</tr><tr><td><input type="button" name="add"  id="createDoc" value="Добавить"></td>   
	<td rowspan="2"><input type="button" id="saveDoc" name="save" value="Сохранить"></td> 
	<td rowspan="2"><input type="button" id="resetDoc" name="reset" value="Сбросить"></td>
	<td rowspan="2"><input type="button" id="cancelDoc" name="cancel" value="Отмена"></td> 
	</tr><tr><td><input type="button" name="del"  id="deleteDoc" value="Удалить"></td></tr>
	</table>
	</form>
	<form id="downloadForm" method="POST" action="/download">
		<input type="hidden" name="id">
	</form>
	<br>
	<br>
	<h2>Форма поиска документов</h2>
	<br>
	<form name="searchForm" method="post">
	<table>
	<tr><td>Категория поиска</td></tr>
	<tr><td>
	<p><select size="1" name="query">
	<option value="name">По имени</option>
	<option value="author">По автору</option>
	<option value="tags">По тэгам</option>
	</select></p>	
	</td></tr>
	<tr><td>Поле для ввода запроса<td></tr>
	<tr><td><input type="text" name="queryField"></td></tr>
	<tr><td><input type="button" id="findButton" value="Найти"></td></tr>
	<tr><td><input type="button" id="clearButton" value="Отменить выделение нодов"></td></tr>
	</table>
	</form>
	<br>
	<div id="result">
		<table id="resulttable">
			<thead>
				<tr>
					<th>Имя документа</th>
					<th>Автор</th>
					<th>Дата последнего изменения</th>
					<th>Наличие файла</th>
					<th>Действие</th>
				</tr>
			</thead>
			<tbody>
			</tbody>
		<table>
	</div>
	</center>
	</td>
	</tr>
	 <center>
  </body>
</HTML>