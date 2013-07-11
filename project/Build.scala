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

  private val kolichSpring = "com.kolich" % "kolich-spring" % "0.0.7" % "compile"

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

  private val quartz = "org.quartz-scheduler" % "quartz" % "1.8.6" % "compile"

  val webAppDeps = Seq(kolichSpring,
    jettyWebApp, jettyPlus, jettyJsp,
    jspApi, jstl, servlet,
    urlrewrite,
    logback, logbackClassic, slf4j, jclOverSlf4j,
    commonsCodec,
    quartz)

}

object Resolvers {

  private val kolichRepo = "Kolich repo" at "http://markkolich.github.com/repo"

  val depResolvers = Seq(kolichRepo)

}

object PackageJS {

  import org.apache.tools.ant.types._
  import com.google.javascript.jscomp.ant._
  
  lazy val packageJS = TaskKey[Unit]("package-js", "Run all JavaScript through Google's Closure compiler.")
  val settings = Seq(
    packageJS <<= baseDirectory(new File(_, "src/main/webapp/WEB-INF/static")) map { base =>
      val js = base / "js"
      val release = base / "release"
      val sources = getFileList(js, Seq("jquery-1.8.3.min.js", "some-javascript-file.js"))
      //val externs = getFileList(js / "externs", Seq("jquery-1.8.js"))
      val compile = getCompileTask(release / "output.js", sources)
      compile.execute
    },
    compile in Compile <<= (compile in Compile) dependsOn (packageJS),
    packageWar in Compile <<= (packageWar in Compile) dependsOn (packageJS))

  private def getCompileTask(output: File, sources: FileList, externs: Option[FileList] = None): CompileTask = {
    val compile = new CompileTask()
    compile.setCompilationLevel("whitespace") // Could be "simple" or "advanced"
    compile.setWarning("quiet") // Could be "verbose"
    compile.setDebug(false)
    compile.setOutput(output)
    compile.addSources(sources)
    if(externs != None) {
      compile.addExterns(externs.get)
    }
    compile
  }

  // Builds a vanilla Ant FileList that contains a list of files
  // in a single directory.
  private def getFileList(dir: File, files: Seq[String]): FileList = {
    val list = new FileList()
    list.setDir(dir)
    list.setFiles(files.mkString(", "))
    list
  }

}

object PackageCSS {

  lazy val packageCSS = TaskKey[Unit]("compile:css", "Run all CSS through YUI's minifier.")
  val settings = Seq(
    packageCSS <<= baseDirectory(new File(_, "src/main/webapp/WEB-INF/static")) map { base =>
      val css = base / "css"
      val release = base / "release"
      // TODO
    },
    compile in Compile <<= (compile in Compile) dependsOn (packageCSS),
    packageWar in Compile <<= (packageWar in Compile) dependsOn (packageCSS))

}

object XSBTWebPluginConfig {

  val settings = webSettings ++
    // Change the location of the packaged WAR file as generated by the
    // xsbt-web-plugin.
    Seq(artifactPath in (Compile, packageWar) ~= { defaultPath =>
      file("dist") / defaultPath.getName
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
      } /*,      
      externalDependencyClasspath in Compile <++= baseDirectory map { base =>
      	val util = base / "util"
      	Seq(util / "yuicompressor-2.4.2.jar",
		    util / "yui-compressor-ant-task-0.5.jar",
		    util / "google-closure-compiler-r2180.jar")
      }*/ ) ++
      // Xsbt-web-plugin settings.
      XSBTWebPluginConfig.settings ++
      // Include the relevant settings for JS and CSS "compilation".
      PackageJS.settings ++ PackageCSS.settings ++
      // Eclipse project plugin settings.
      SBTEclipsePluginConfig.settings)

}
