trait valid_both_in_bash_and_in_scala /* 2>/dev/null
# ^^^ can be replaced by anything that's harmless in Bash and valid top-file-def in Scala.
# Kudos to that attendee (sorry, I don't know your name), who noticed that opportunity. Cheers!

set -e

function fetch() {
  FILE=$1
  URL=$2
  echo  "wget -q -O $FILE $URL"
  wget -q -O $FILE $URL
  if [ ! -s $file ] ; then
    # Run wget without --quiet to get errors
    echo "Failed download of file $FILE fetched from url $URL"
    wget -O $FILE $URL
    if [ ! -s $file ] ; then
      rm $FILE
      exit 1
    fi
  fi
}

# Making sure Coursier is available
cr=~/.coursier
test -e $cr/cr || (mkdir -p $cr && fetch $cr/cr https://git.io/vgvpD && chmod +x $cr/cr)

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

if [ $(uname -o) = "Cygwin" ] ; then
  echo "I don't work on Cygwin (due to ammonite-repl and coursier dependency)"
  exit 1
fi
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

    val port: Int = 9789
    val ipAddress = "127.0.0.1"
    val server = NettyServer.fromRouter(ServerConfig(
      port = Some(port),
      address = ipAddress
    )) {
      case GET(p"/hello/$to") => Action { implicit req =>
        Results.Ok(s"Hello $to ${req.host}")
      }
    }
    println(s"Server started! Please go to http://$ipAddress:$port/hello/world to see the result")
    
    readLine
    
    server.stop()
  }
}
