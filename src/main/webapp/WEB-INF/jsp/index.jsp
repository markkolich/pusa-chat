<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>

<%
// The copyright year is dynamic, always set to the current year.
// This is so we don't have to change the messages file each year,
// it just changes itself.
pageContext.setAttribute("year", new SimpleDateFormat("yyyy").format(new Date()));
%>

<!doctype html>
<html>

	<jsp:include page="includes/page-header.jsp" />
	
	<body class="homepage">
	
		<h2>Pusa Chat</h2>
		<div class="panel join" style="display:block;">
			<h3>Join Room</h3>
			<form method="get" action="#" class="join" autocomplete="off">
				<label for="join-key">Room Key:</label>
				<input type="text" value="${key}" name="key" id="join-key" maxlength="36" />
				<input type="submit" value="Join Room" />
			</form>
			<p class="direct"><a href="javascript:void(0)" class="create">Create new room</a></p>
		</div>
		<div class="panel create" style="display:none;">
			<h3>Create New Room</h3>
			<form method="get" action="#" class="create" autocomplete="off">
				<label for="create-key">Room Key:</label>
				<input type="text" value="" name="key" id="create-key" maxlength="36" />
				<p class="url" style="display:none;">&nbsp;</p>
				<p class="buttons"><input type="submit" value="Create Room" />&nbsp;<input type="button" class="random" value="Generate Random" /></p>
			</form>
			<p class="direct"><a href="javascript:void(0)" class="join">Join room</a></p>
		</div>
		
		<h4>&copy; Copyright <c:out value="${year}" /> Mark S. Kolich</h4>
		
		<jsp:include page="includes/scripts.jsp" />
			
	</body>
</html>