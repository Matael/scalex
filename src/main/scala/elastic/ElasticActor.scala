package org.scalex
package elastic

import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import scala.util.{ Try, Success, Failure }

import akka.actor._
import com.typesafe.config.Config
import org.elasticsearch.action.search.SearchResponse
import play.api.libs.json.{ Json, JsObject }
import scalastic.elasticsearch.Indexer

import api._
import util.Timer._

private[scalex] final class ElasticActor(config: Config) extends Actor {

  private val indexName = config getString "index"

  var indexer: Indexer = _

  override def preStart {
    indexer = instanciateIndexer
  }

  override def postStop {
    println("[search] Stopping indexer")
    Option(indexer) foreach { _.stop() }
  }

  def receive = akka.event.LoggingReceive {

    case api.Clear(typeName, mapping) ⇒ Await.ready((Future {
      try {
        indexer.deleteByQuery(Seq(indexName), Seq(typeName))
        indexer.deleteMapping(indexName :: Nil, typeName.some)
      }
      catch {
        case e: org.elasticsearch.indices.TypeMissingException ⇒
      }
      indexer.putMapping(indexName, typeName, Json stringify Json.obj(typeName -> mapping))
      indexer.refresh()
    }) recover { case e ⇒ println("[elastic] clear: " + e) }, 3 second)

    case api.Optimize ⇒ sender ! {
      indexer.refresh(Seq(indexName))
      indexer.optimize(Seq(indexName))
    }

    case api.IndexMany(typeName, docs) ⇒
      indexer bulk {
        docs map {
          case (id, doc) ⇒
            indexer.index_prepare(
              indexName,
              typeName,
              id,
              Json stringify doc
            ).request
        }
      }

    case req: api.Request[_] ⇒ sender ! req.in(indexName)(indexer)
  }

  private def instanciateIndexer = {
    println("[search] Start indexer")
    val i = Indexer.transport(
      settings = Map("cluster.name" -> config.getString("cluster")),
      host = config getString "host",
      ports = Seq(config getInt "port"))
    i.start
    try {
      i.createIndex(indexName, settings = Map(
        "index.mapper.dynamic" -> "false"
      ))
    }
    catch {
      case e: org.elasticsearch.indices.IndexAlreadyExistsException ⇒
    }
    println("[search] Indexer is running")
    i
  }
}
