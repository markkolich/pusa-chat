<#--
  Every error page. statusMessage is the standard reason phrase ("Not Found"); the exception's
  message is only shown in dev mode. Rendered without the shared layout and its scripts: there's
  nothing to do on an error page.
-->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>${statusCode} ${statusMessage!}</title>
<#if devMode>
    <link rel="stylesheet" href="${contextPath}/static/build/app.css?v=${.now?long?c}">
<#else>
    <link rel="stylesheet" href="${contextPath}/static/release/app.min.css?v=${buildVersion.buildNumber}">
</#if>
    <link rel="icon" href="${contextPath}/favicon.ico" type="image/x-icon">
</head>
<body class="error-page">
<h2>${statusCode} ${statusMessage!}</h2>
<#if devMode && message??>
<p>${message}</p>
</#if>
<p><a href="${contextPath}/">Pusa Chat</a></p>
</body>
</html>
