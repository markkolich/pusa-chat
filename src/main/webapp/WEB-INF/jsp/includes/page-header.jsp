<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ taglib prefix="parameter" uri="com.kolich.pusachat.tags.parameter" %>
<%@ taglib prefix="util" uri="com.kolich.pusachat.tags.util" %>

<head>
	<meta http-equiv="content-type" content="text/html; charset=UTF-8">
	<title><c:choose><c:when test='${!empty param.title}'><c:out value="${param.title}" /></c:when><c:otherwise>Pusa Chat</c:otherwise></c:choose></title>
	
	<parameter:mode bindToVar="mode" />
	<c:choose>
		<c:when test='${mode == "production"}'>
			<link rel="stylesheet" href="<parameter:context-path />css/pusachat.css?v=<parameter:version />" type="text/css" />
		</c:when>
		<c:otherwise>
			<!-- DEV MODE -->
			<link rel="stylesheet" href="<parameter:context-path />css/pusachat.css?debug=true&v=_<util:epoch />" type="text/css" />
		</c:otherwise>
	</c:choose>

	<link rel="icon" href="<parameter:context-path />favicon.ico" type="image/x-icon" />
	<link rel="shortcut icon" href="<parameter:context-path />favicon.ico" type="image/x-icon" />
</head>