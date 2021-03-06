package ramomar

import scala.concurrent.Future

import com.sksamuel.elastic4s.http.Response
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.sksamuel.elastic4s.http.index.IndexResponse

package object elasticsearch {
  case class Document(index: String, `type`: String, fields: Seq[(String, Any)])

  trait ElasticsearchService {
    def indexDocument(document: Document): Future[Response[IndexResponse]]
    def indexDocuments(documents: Seq[Document]): Future[Response[BulkResponse]]
  }
}
