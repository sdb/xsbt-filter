package sbtfilter

import sbt._

object ImageFileFilter extends FileFilter {
  val formats = Seq("jpg", "jpeg", "png", "gif", "bmp")
  def accept(file: File) = formats contains file.ext.toLowerCase
}

object Filter {
  import scala.util.matching.Regex._
  import java.io.{ FileReader, BufferedReader, PrintWriter }

  val pattern = """(^|[^\\])(\$\{[^}]+?\})""".r
  def replacer(log: Logger, props: Map[String, String])(m: Match): Option[String] = {
    val name = m.group(2).drop(2).dropRight(1)
    props.get(name).map(m.group(1) + filter(log, _, props))
  }

  def filter(log: Logger, line: String, props: Map[String, String]): String =
    try pattern.replaceSomeIn(line, replacer(log, props))
    catch {
      case e: Throwable =>
        log.error(s"Failed to filter line: $line")
        log.trace(e)
        throw e
    }

  def apply(log: Logger, files: Seq[File], props: Map[String, String]) {
    log.debug(s"Filter properties: ${props.toSeq.sorted.mkString("{\n  ", ",\n  ", "\n}")}")
    IO.withTemporaryDirectory { dir =>
      files.foreach { src =>
        try {
          log.warn(s"Filtering $src")
          val dest = new File(dir, src.getName)
          val out = new PrintWriter(dest)
          val in = new BufferedReader(new FileReader(src))
          IO.foreachLine(in) { line => IO.writeLines(out, Seq(filter(log, line, props))) }
          in.close()
          out.close()
          IO.copyFile(dest, src, preserveLastModified = true)
          println(IO.read(dest))
          true
        } catch {
          case e: Throwable =>
            log.error(s"Failed to filter $src")
            log.trace(e)
            false
        }
      }
    }
  }
}
