resolvers := Seq("xsbt-web-plugin repo" at "http://siasia.github.com/maven2")

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.3.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.2.0")

// Add dependencies for JS and CSS "compilation".
// Note that this adds the dependencies only for the build process (makes
// these libraries available to the build classpath, namely in Build.scala).
// This does ~not~ add the dependencies to the compile time or run time classpaths.

libraryDependencies += "com.google.javascript" % "closure-compiler" % "r2180"

libraryDependencies += "com.yahoo.platform.yui" % "yuicompressor" % "2.4.7"
