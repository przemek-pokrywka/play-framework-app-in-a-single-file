trait valid_both_in_bash_and_in_scala /* 2>/dev/null
# ^^^ can be replaced by anything that's harmless in Bash and valid top-file-def in Scala.
# Kudos to that attendee (sorry, I don't know your name), who noticed that opportunity. Cheers!

# Making sure Coursier is available
cr=~/.coursier
test -e $cr/cr || (mkdir $cr && wget -q -O $cr/cr https://git.io/vgvpD && chmod +x $cr/cr)

dependencies=(
  com.typesafe.play:play-netty-server_2.11:2.5.0
  com.typesafe.play:play_2.11:2.5.0

  com.lihaoyi:ammonite-repl_2.11.7:0.5.5    # Mandatory for running Scala script
)

# Generate simple build.sbt for editing in IDEs locally. Run with `--build.sbt` (now also on Macs)
test "$1" == "--build.sbt" && \
  printf '%s\n' "${dependencies[@]}" | \
  sed 's/\(.*\):\(.*\):\(.*\)/libraryDependencies += "\1" % "\2" % "\3"/g' > build.sbt && \
  exit

# Small enhancement to the Scalapolis version. Enabling Ammonite to cache compilation output:
just_scala_file=${TMPDIR:-/tmp}/$(basename $0)
(sed -n '/^object script/,$ p' $0; echo "script.run()") > $just_scala_file

CLASSPATH="$($cr/cr fetch -q -p ${dependencies[*]} )" \
  java \
    -Dplay.crypto.secret=foo.bar.baz \
    -Dconfig.resource=reference.conf \
    ammonite.repl.Main $just_scala_file # hide Bash part from Ammonite
                                        # and make it run the Scala part

exit $?
# */

object script {
  def run() {
    import play.core.server._
    import play.api.routing.sird._
    import play.api.mvc._

    val port: Int = 9000
    val server = NettyServer.fromRouter(ServerConfig(
      port = Some(port),
      address = "127.0.0.1"
    )) {
      case GET(p"/hello/$to") => Action { implicit req =>
        Results.Ok(s"Hello $to ${req.host}")
      }
    }
    println("Server started! Please go to http://127.0.0.1:9000/hello/world to see the result")
    
    readLine
    
    server.stop()
  }
}
