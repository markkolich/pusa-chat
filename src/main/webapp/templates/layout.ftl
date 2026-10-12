<#--
  The frame every page shares: <head>, then the page's body content, then the scripts.
  Usage:

    <#import "layout.ftl" as layout>
    <@layout.page title="Pusa Chat" bodyClass="homepage">
      ...page content...
    </@layout.page>

  The scripts find the app's base path in the <body>'s data-context-path attribute.

  The CSS and JavaScript are built by gulp (src/main/webapp/static/gulpfile.js): in dev mode the
  pages load the concatenated build/app.{css,js}, otherwise the minified release/app.min.{css,js}.
-->
<#macro page title bodyClass="" bodyId="">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>${title}</title>
<#if devMode>
    <link rel="stylesheet" href="${contextPath}/static/build/app.css?v=${.now?long?c}">
<#else>
    <link rel="stylesheet" href="${contextPath}/static/release/app.min.css?v=${buildVersion.buildNumber}">
</#if>
    <link rel="icon" href="${contextPath}/favicon.ico" type="image/x-icon">
</head>
<body<#if bodyClass?has_content> class="${bodyClass}"</#if><#if bodyId?has_content> id="${bodyId}"</#if> data-context-path="${contextPath}">
<#nested>
<#if devMode>
<script src="${contextPath}/static/build/app.js?v=${.now?long?c}"></script>
<#else>
<script src="${contextPath}/static/release/app.min.js?v=${buildVersion.buildNumber}"></script>
</#if>
</body>
</html>
</#macro>
