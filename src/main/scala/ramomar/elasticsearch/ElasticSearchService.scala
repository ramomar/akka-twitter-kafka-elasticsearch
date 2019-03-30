package ramomar.elasticsearch

import scala.concurrent.{ExecutionContext, Future}

import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.Response
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.sksamuel.elastic4s.indexes.IndexRequest

class ElasticSearch(client: ElasticClient)
                   (implicit protected val ec: ExecutionContext)
  extends ElasticSearchService {

  import com.sksamuel.elastic4s.http.ElasticDsl._

  def indexDocument(document: Document): Future[Response[IndexResponse]] =
  client.execute {
    indexIntoQuery(document)
  }

  def indexDocuments(documents: Seq[Document]) : Future[Response[BulkResponse]] =
    client.execute {
      bulk(documents.map(indexIntoQuery))
    }

  private def indexIntoQuery(document: Document): IndexRequest =
    indexInto(document.index, document.`type`).fields(document.fields: _*)
}
