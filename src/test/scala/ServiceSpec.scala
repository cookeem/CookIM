
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.CookIM
import com.cookeem.chat.restful.RouteOps._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{TextMessage, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json


/**
  * Created by cookeem on 16/12/21.
  */
class ServiceSpec extends WordSpec with Matchers with ScalatestRouteTest {
  CookIM.main("-h localhost -w 8080 -a 2551 -s localhost:2551".split(" "))

  val wsClient = WSProbe()

  "Service" should {
    "respond to get file" in {
      Get("/api/getFile?id=1") ~> routeGetFile ~> check {
        println(response)
//        responseAs[String] shouldEqual "Captain on the bridge!"
        status shouldEqual StatusCodes.OK
      }
    }
    "respond to websocket chat" in {
      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()
      val wsClientFlow = Http().webSocketClientFlow(WebSocketRequest("ws://localhost:8080/ws-chat"))
      Source[Int](List(1, 2, 3))
        .via(Flow[Int].map[Message](i => TextMessage(i.toString)))
        .via(wsClientFlow)
        .via(
          Flow[Message].collect[String] {
            case TextMessage.Strict(txt) =>
              val json = Json.parse(txt)
              val msgType = getJsonString(json, "msgType")
              msgType
            case BinaryMessage.Strict(_) =>
              "error"
          }
        )
        .runWith(TestSink.probe[String])
        .request(3)
        .expectNext("reject", "reject", "reject")
        .expectComplete()
    }
  }
}
