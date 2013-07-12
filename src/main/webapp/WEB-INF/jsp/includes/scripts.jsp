<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:choose>
	<c:when test='${PusaChatProperties.isProductionMode()}'>
		<script src="${PusaChatProperties.getContextPath()}js/pusachat.js?v=${PusaChatProperties.getAppVersion()}"></script>
	</c:when>
	<c:otherwise>
		<!-- DEV MODE -->
		<script src="${PusaChatProperties.getContextPath()}js/lib/json2.js?debug=true"></script>
		<script src="${PusaChatProperties.getContextPath()}js/lib/jquery-1.7.1.min.js?debug=true"></script>
		<script src="${PusaChatProperties.getContextPath()}js/lib/jquery.chrono-1.1.js?debug=true"></script>
		<script src="${PusaChatProperties.getContextPath()}js/lib/jquery.simplemodal-1.4.1.js?debug=true"></script>
		<script src="${PusaChatProperties.getContextPath()}js/lib/jquery.titlealert-0.7.js?debug=true"></script>
		<script src="${PusaChatProperties.getContextPath()}js/lib/jquery.typing-0.2.0.min.js?debug=true"></script>
		<script src="${PusaChatProperties.getContextPath()}js/lib/jquery.localtime-0.5.js?debug=true"></script>
		<script src="${PusaChatProperties.getContextPath()}js/pusachat.js?debug=true"></script>
		<script src="${PusaChatProperties.getContextPath()}js/pusachat.chat.js?debug=true"></script>
		<script src="${PusaChatProperties.getContextPath()}js/pusachat.homepage.js?debug=true"></script>
	</c:otherwise>
</c:choose>