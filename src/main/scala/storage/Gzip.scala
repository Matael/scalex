package org.scalex
package storage

import java.io.{ File ⇒ _, _ }
import java.util.zip.{ GZIPOutputStream, GZIPInputStream }
import scala.concurrent.Future

private[storage] trait Gzip[H, A] {

  protected val encoding = "UTF-8"

  def bufferedInputStream(file: File)(f: BufferedReader ⇒ H): Future[H] = Future {
    val fileIn = new FileInputStream(file)
    val decoder = new InputStreamReader(fileIn, encoding)
    val buffered = new BufferedReader(decoder)
    try {
      f(buffered)
    }
    finally {
      buffered.close()
      decoder.close()
      fileIn.close()
    }
  }

  def inputStream(file: File)(f: InputStream ⇒ InputStream ⇒ A): Future[A] = Future {
    val fileIn = new FileInputStream(file)
    val gzip = new GZIPInputStream(fileIn)
    try {
      f(fileIn)(gzip)
    }
    finally {
      gzip.close()
      fileIn.close()
    }
  }

  def outputStream(file: File, a: A)(f: OutputStream ⇒ OutputStream ⇒ Unit) {
    file.delete()
    val fileOut = new FileOutputStream(file)
    val gzip = new GZIPOutputStream(fileOut)
    try {
      f(fileOut)(gzip)
    }
    finally {
      gzip.close()
      fileOut.close()
    }
  }
}
