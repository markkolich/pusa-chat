<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ taglib prefix="parameter" uri="com.kolich.pusachat.tags.parameter" %>
<%@ taglib prefix="util" uri="com.kolich.pusachat.tags.util" %>

<parameter:mode bindToVar="mode" />
<c:choose>
	<c:when test='${mode == "production"}'>
		<script src="<parameter:context-path />js/pusachat.js?v=<parameter:version />"></script>
	</c:when>
	<c:otherwise>
		<!-- DEV MODE -->
		<script src="<parameter:context-path />js/lib/json2.js?debug=true&v=_<util:epoch />"></script>
		<script src="<parameter:context-path />js/lib/jquery-1.7.1.min.js?debug=true&v=_<util:epoch />"></script>
		<script src="<parameter:context-path />js/lib/jquery.chrono-1.1.js?debug=true&v=_<util:epoch />"></script>
		<script src="<parameter:context-path />js/lib/jquery.simplemodal-1.4.1.js?debug=true&v=_<util:epoch />"></script>
		<script src="<parameter:context-path />js/lib/jquery.titlealert-0.7.js?debug=true&v=_<util:epoch />"></script>
		<script src="<parameter:context-path />js/lib/jquery.typing-0.2.0.min.js?debug=true&v=_<util:epoch />"></script>
		<script src="<parameter:context-path />js/lib/jquery.localtime-0.5.js?debug=true&v=_<util:epoch />"></script>
		<script src="<parameter:context-path />js/pusachat.js?debug=true&v=_<util:epoch />"></script>
		<script src="<parameter:context-path />js/pusachat.chat.js?debug=true&v=_<util:epoch />"></script>
		<script src="<parameter:context-path />js/pusachat.homepage.js?debug=true&v=_<util:epoch />"></script>
	</c:otherwise>
</c:choose>