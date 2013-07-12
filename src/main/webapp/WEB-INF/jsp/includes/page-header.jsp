<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<head>
	<meta http-equiv="content-type" content="text/html; charset=UTF-8">
	<title><c:choose><c:when test='${!empty param.title}'><c:out value="${param.title}" /></c:when><c:otherwise>Pusa Chat</c:otherwise></c:choose></title>
	
	<c:choose>
		<c:when test='${PusaChatProperties.isProductionMode()}'>
			<link rel="stylesheet" href="${PusaChatProperties.getContextPath()}css/pusachat.css??v=${PusaChatProperties.getAppVersion()}" type="text/css" />
		</c:when>
		<c:otherwise>
			<!-- DEV MODE -->
			<link rel="stylesheet" href="${PusaChatProperties.getContextPath()}css/pusachat.css?debug=true&?v=${PusaChatProperties.getAppVersion()}" type="text/css" />
		</c:otherwise>
	</c:choose>

	<link rel="icon" href="${PusaChatProperties.getContextPath()}favicon.ico" type="image/x-icon" />
	<link rel="shortcut icon" href="${PusaChatProperties.getContextPath()}favicon.ico" type="image/x-icon" />
</head>