<%@ page import="org.eclipse.help.servlet.*" errorPage="err.jsp" contentType="text/html; charset=UTF-8"%>

<% 
	// calls the utility class to initialize the application
	application.getRequestDispatcher("/servlet/org.eclipse.help.servlet.InitServlet").include(request,response);
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!--
 (c) Copyright IBM Corp. 2000, 2002.
 All Rights Reserved.
-->
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">

	<!--
	<base target="NavFrame.document.all.search">
	-->
     
<style type="text/css">


BODY {
	background-color:#ffffff; /*#D4D0C8;*/
	text:white;
	height:100%;
	width:100%;
}

TABLE {
	font: 8pt Tahoma;
	background:#ffffff;
	margin:0;
	border:1px solid black;
	padding:0;
	height:100%;
}

FORM {
	margin:0;
}

INPUT {
	font: 8pt Tahoma;
	border:0px;
	margin:0px;
	padding:0px;
}


#searchTable {
	margin-right:4px;
}

#searchWord {	
	border:0px;
	padding-left:4px;
	padding-right:4px;
}

#go {
	font-weight:bold;
}


#advanced {
	text-decoration:underline; 
	text-align:right;
	color:#0000ff; 
	cursor:hand;
	margin-left:4px;
	border:0px;
}

</style>

<script language="JavaScript">

var selectedBooks;
var advancedDialog;
var w = 580;
var h = 400;

function saveSelectedBooks(books)
{
	selectedBooks = new Array(books.length);
	for (var i=0; i<selectedBooks.length; i++){
		selectedBooks[i] = new String(books[i]);
	}
}

function openAdvanced()
{
	advancedDialog = window.open("advanced.jsp?searchWordJS13="+escape(document.getElementById("searchWord").value), "advancedDialog", "height="+h+",width="+w );
	advancedDialog.focus(); 
}

function closeAdvanced()
{
	if (advancedDialog)
		advancedDialog.close();
}

function doSearch()
{
	var form = document.forms["searchForm"];
	var searchWord = form.searchWord.value;
	var maxHits = form.maxHits.value;
	if (!searchWord || searchWord == "")
		return;
	else
		parent.doSearch("searchWordJS13="+escape(searchWord)+"&maxHits="+maxHits);
}

</script>

</head>

<body onunload="closeAdvanced()">

	<form  name="searchForm"   onsubmit="doSearch()">
		<table id="searchTable" align="left" valign="top" cellspacing="0" cellpadding="0" border="0">
			<tr nowrap  valign="middle">
				<td>
					&nbsp;<%=WebappResources.getString("Search", request)%>:
				</td>
				<td>
					<input type="text" id="searchWord" name="searchWord" value="<%= UrlUtil.getRequestParameter(request, "searchWord")!=null?UrlUtil.getRequestParameter(request, "searchWord"):""%>" size="20" maxlength="256" alt='<%=WebappResources.getString("SearchExpression", request)%>'>
				</td>
				<td >
					&nbsp;<input type="button" onclick="this.blur();doSearch()" value='<%=WebappResources.getString("GO", request)%>' id="go" alt='<%=WebappResources.getString("GO", request)%>'>
					<input type="hidden" name="maxHits" value="500" >
				</td>
				<td>
					&nbsp;<a id="advanced" href="javascript:openAdvanced();" alt='<%=WebappResources.getString("Advanced", request)%>'><%=WebappResources.getString("Advanced", request)%></a>&nbsp;
				</td>
			</tr>

		</table>
	</form>		

</body>
</html>

