/**
 * Copyright (c) 2013 Mark S. Kolich
 * http://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import sbt._
import sbt.Keys._

import com.earldouglas.xsbtwebplugin._
import PluginKeys._
import WebPlugin._
import WebappPlugin._

import com.typesafe.sbteclipse.plugin.EclipsePlugin._

object Dependencies {

  // Internal dependencies

  private val kolichSpring = "com.kolich" % "kolich-spring" % "0.0.7" % "compile" exclude("com.kolich", "kolich-common")
  private val kolichCommon = "com.kolich" % "kolich-common" % "0.1.0" % "compile"

  // External dependencies

  // Using Jetty 8 "stable", version 8.1.8.v20121106
  private val jettyWebApp = "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "container"
  private val jettyPlus = "org.eclipse.jetty" % "jetty-plus" % "8.1.8.v20121106" % "container"
  private val jettyJsp = "org.eclipse.jetty" % "jetty-jsp" % "8.1.8.v20121106" % "container"

  private val jspApi = "javax.servlet.jsp" % "jsp-api" % "2.2" % "provided" // Provided by servlet container
  private val jstl = "jstl" % "jstl" % "1.2" % "compile" // Package with WAR
  private val servlet = "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided" // Provided by container

  private val urlrewrite = "org.tuckey" % "urlrewritefilter" % "3.2.0" % "compile"

  private val logback = "ch.qos.logback" % "logback-core" % "1.0.7" % "compile"
  private val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.0.7" % "compile" // An Slf4j impl
  private val slf4j = "org.slf4j" % "slf4j-api" % "1.6.4" % "compile"
  private val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % "1.6.6" % "compile"

  private val commonsCodec = "commons-codec" % "commons-codec" % "1.6" % "compile"
  private val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.1" % "compile"

  val webAppDeps = Seq(kolichSpring, kolichCommon,
    jettyWebApp, jettyPlus, jettyJsp,
    jspApi, jstl, servlet,
    urlrewrite,
    logback, logbackClassic, slf4j, jclOverSlf4j,
    commonsCodec, commonsLang3)

}

object Resolvers {

  private val kolichRepo = "Kolich repo" at "http://markkolich.github.com/repo"

  val depResolvers = Seq(kolichRepo)

}

object PackageJs {

  import org.apache.tools.ant.types._
  import org.apache.tools.ant.taskdefs._
  
  import com.google.javascript.jscomp.ant._

  private lazy val packageJs = TaskKey[Unit](
      "package:js",
      "Compile and package application JavaScript with Google's Closure Compiler."
  )
  
  val settings = Seq(
    packageJs <<= baseDirectory(new File(_, "src/main/webapp/WEB-INF/static")) map { base =>
      val js = base / "js"
      val build = js / "build"
      val release = base / "release"
      
      val sources = getFileList(js, Seq(
        "pusachat.js",
        "pusachat.homepage.js",
        "pusachat.chat.js"))
        
      val externs = getFileList(js / "externs", Seq(
        "jquery-1.7.js",
        "jquery.chrono-1.1.js",
        "jquery.localtime-0.5.js",
        "jquery.simplemodal-1.4.1.js",
        "jquery.titlealert-0.7.js",
        "jquery.typing-0.2.0.min.js"))
        
      val libs = getFileList(js / "lib", Seq(
        "json2.js",
        "jquery-1.7.1.min.js",
        "jquery.chrono-1.1.js",
        "jquery.localtime-0.5.js",
        "jquery.simplemodal-1.4.1.js",
        "jquery.titlealert-0.7.js",
        "jquery.typing-0.2.0.min.js"))
        
      println("Compiling JavaScript...")
      
      // Concatenate the library related JavaScript files together.
      // Compile using the "simple" compilation level. 
      concatenate(build / "pusachat.lib.js", libs)
      closureCompile(build / "pusachat.lib.js", getFileList(build, "pusachat.lib.js"))

      // Package the core app JavaScript files together.
      // Compile using the "advanced" compilation level.
      concatenate(build / "pusachat.js", sources)
      closureCompile(build / "pusachat.js", getFileList(build, "pusachat.js"),
        Some(externs), "advanced")

      // Package the final product, the libraries and the application
      // JavaScript core, into a single deliverable.
      concatenate(release / "pusachat.js",
        getFileList(build, Seq("pusachat.lib.js", "pusachat.js")))
    },
    compile in Compile <<= (compile in Compile) dependsOn (packageJs),
    packageWar in Compile <<= (packageWar in Compile) dependsOn (packageJs)
  )

  private def closureCompile(output: File, sources: FileList,
    externs: Option[FileList] = None, compilationLevel: String = "simple") {
    val compile = new CompileTask()
    compile.setCompilationLevel(compilationLevel) // Could be "simple" or "advanced"
    compile.setWarning("quiet") // Could be "verbose"
    compile.setDebug(false)
    compile.setOutput(output)
    compile.addSources(sources)
    compile.setForceRecompile(false) // False is the default, but here for doc purposes
    if (externs != None) {
      compile.addExterns(externs.get)
    }
    compile.execute
  }
  private def getFileList(dir: File, files: Seq[String]): FileList = {
    val list = new FileList()
    list.setDir(dir)
    list.setFiles(files.mkString(", "))
    list
  }
  private def getFileList(dir: File, file: String): FileList = {
    getFileList(dir, Seq(file))
  }
  private def concatenate(dest: File, fileList: FileList) {
    val concat = new Concat()
    concat.setDestfile(dest)
    concat.addFilelist(fileList)
    concat.execute
  }
}

object PackageCss {
  
  import java.io._
  
  import com.yahoo.platform.yui.compressor._

  private lazy val packageCss = TaskKey[Unit](
      "package:css",
      "Minify CSS using YUI's CSS compressor."
  )
  
  val settings = Seq(
    packageCss <<= baseDirectory(new File(_, "src/main/webapp/WEB-INF/static")) map { base =>
      val css = base / "css"
      val release = base / "release"
      // Create the "release" directory if it does not exist (YUI does not
      // create this destination directory for us... it has to exist before
      // we attempt to use it).
      IO.createDirectory(release)
      println("Compiling CSS...")
      // Setup the input reader and the output writer.
      var reader:Reader = null
      var writer:Writer = null
      try {
        reader = new InputStreamReader(new FileInputStream(css / "pusachat.css"), "UTF-8");
        writer = new OutputStreamWriter(new FileOutputStream(release / "pusachat.css"), "UTF-8")
        new CssCompressor(reader).compress(writer, -1)
      } finally {
        if(reader != null) { reader.close }
        if(writer != null) { writer.close }
      }
    },
    compile in Compile <<= (compile in Compile) dependsOn (packageCss),
    packageWar in Compile <<= (packageWar in Compile) dependsOn (packageCss)
  )

}

object XSBTWebPluginConfig {

  val settings = webSettings ++ Seq(
    // Change the location of the packaged WAR file as generated by the
    // xsbt-web-plugin.
    artifactPath in (Compile, packageWar) ~= { defaultPath =>
    	file("dist") / defaultPath.getName
    },
    warPostProcess in Compile <<= (target) map {
    	// Specific directories that contain intermediate build files
	    // and artifacts should not make their way into the packaged WAR
	    // file.  As such, this removes temporary directories used just for
	    // the build.
	    (target) => { () => {
	      val webinf = target / "webapp" / "WEB-INF"
	      IO.delete(webinf / "work") // recursive
	      IO.delete(webinf / "static" / "js" / "build") // recursive
	    }}
    })

}

object SBTEclipsePluginConfig {

  import com.typesafe.sbteclipse.plugin.EclipsePlugin._

  val settings = Seq(EclipseKeys.createSrc := EclipseCreateSrc.Default,
    // Make sure SBT also fetches/loads the "src" (source) JAR's for
    // all declared dependencies.
    EclipseKeys.withSource := true,
    // This is a Java project, only.
    EclipseKeys.projectFlavor := EclipseProjectFlavor.Java)

}

object PusaChat extends Build {

  import Dependencies._
  import Resolvers._

  private val aName = "pusa-chat"
  private val aVer = "1.0"
  private val aOrg = "com.kolich"

  lazy val pusaChat: Project = Project(
    aName,
    new File("."),
    settings = Defaults.defaultSettings ++ Seq(resolvers := depResolvers) ++ Seq(
      version := aVer,
      organization := aOrg,
      scalaVersion := "2.10.1",
      javacOptions ++= Seq("-Xlint", "-g"),
      shellPrompt := { (state: State) => { "%s:%s> ".format(aName, aVer) } },
      // Set list of dependencies and ask SBT to retreive sources for all
      // managed libraries/dependencies.
      libraryDependencies ++= webAppDeps,
      retrieveManaged := true,
      // True to export the packaged JAR instead of just the compiled .class files.
      exportJars := true,
      // Disable using the Scala version in output paths and artifacts.
      // When running 'publish' or 'publish-local' SBT would append a
      // _<scala-version> postfix on artifacts. This turns that postfix off.
      crossPaths := false,
      // Keep the scala-lang library out of the generated POM's for this artifact. 
      autoScalaLibrary := false,
      // Only add src/main/java and src/test/java as source folders in the project.
      // Not a "Scala" project at this time.
      unmanagedSourceDirectories in Compile <<= baseDirectory(new File(_, "src/main/java"))(Seq(_)),
      unmanagedSourceDirectories in Test <<= baseDirectory(new File(_, "src/test/java"))(Seq(_)),
      // Tell SBT to include our .java files when packaging up the source JAR.
      unmanagedSourceDirectories in Compile in packageSrc <<= baseDirectory(new File(_, "src/main/java"))(Seq(_)),
      // Override the SBT default "target" directory for compiled classes.
      classDirectory in Compile <<= baseDirectory(new File(_, "target/classes")),
      // Add the local 'config' directory to the classpath at runtime,
      // so anything there will ~not~ be packaged with the application deliverables.
      // Things like application configuration .properties files go here in
      // development and so these will not be packaged+shipped with a build.
      // But, they are still available on the classpath during development,
      // like when you run Jetty via the xsbt-web-plugin that looks for some
      // configuration file or .properties file on the classpath.
      unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "config") },
      // Do not bother trying to publish artifact docs (scaladoc, javadoc). Meh.
      publishArtifact in packageDoc := false,
      // Override the global name of the artifact.
      artifactName <<= (name in (Compile, packageBin)) { projectName =>
        (config: ScalaVersion, module: ModuleID, artifact: Artifact) =>
          var newName = projectName
          if (module.revision.nonEmpty) {
            newName += "-" + module.revision
          }
          newName + "." + artifact.extension
      },
      // Override the default 'package' path used by SBT. Places the resulting
      // JAR into a more meaningful location.
      artifactPath in (Compile, packageBin) ~= { defaultPath =>
        file("dist") / defaultPath.getName
      },
      // Override the default 'test:package' path used by SBT. Places the
      // resulting JAR into a more meaningful location.
      artifactPath in (Test, packageBin) ~= { defaultPath =>
        file("dist") / "test" / defaultPath.getName
      }) ++
      // Xsbt-web-plugin settings.
      XSBTWebPluginConfig.settings ++
      // Include the relevant settings for JS and CSS "compilation".
      PackageJs.settings ++ PackageCss.settings ++
      // Eclipse project plugin settings.
      SBTEclipsePluginConfig.settings)

}
