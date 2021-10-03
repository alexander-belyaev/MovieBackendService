package by.bsu.web.api


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.scaladsl.FileIO
import by.bsu.Application.LOGGER
import by.bsu.model.repository.{Film, NewFilmWithFields, NewFilmWithFieldsId, NewFilmWithId}
import by.bsu.utils.RouteService.{filmsParserService, filmsService}
import by.bsu.web.api.rejections.CustomRejectionHandler
import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny}

import java.nio.file.Paths
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

trait FilmJsonMapping extends DefaultJsonProtocol {
  implicit val film1Format: RootJsonFormat[NewFilmWithId] = jsonFormat14(NewFilmWithId.apply)
  implicit val film2Format: RootJsonFormat[Film] = jsonFormat10(Film.apply)
  implicit val film3Format: RootJsonFormat[NewFilmWithFields] = jsonFormat14(NewFilmWithFields.apply)
  implicit val film4Format: RootJsonFormat[NewFilmWithFieldsId] = jsonFormat14(NewFilmWithFieldsId.apply)
}

trait FilmsApi extends FilmJsonMapping with CommentsApi with CustomRejectionHandler {
  val filmRoute: Route = {
    delete {
      (pathPrefix(IntNumber)) { id => {
        LOGGER.debug(s"Deleting a film with $id id")
        complete(filmsService.deleteById(id).map(_.toJson))
      }
      }
    } ~
      get {
        pathPrefix("file-example") {
          complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, FileIO.fromPath(Paths.get("src/main/resources/films-file-example.csv"))))
        } ~
          pathPrefix("private") {
            LOGGER.debug("Getting all private films")
            complete(filmsService.getAllPrivate)
          } ~
          (pathPrefix(IntNumber)) { id => {
            LOGGER.debug(s"Getting films with $id id")
            complete(filmsService.getById(id).map(_.toJson))
          }
          }
      } ~
      (pathPrefix(IntNumber) & put) { id =>
        entity(as[Film]) { entity => {
          LOGGER.debug(s"Updating a new film with $id id")
          complete(filmsService.updateById(id, entity).map(_.toJson))
        }
        }
      } ~
      post {
        entity(as[NewFilmWithId]) { entity => {
          LOGGER.debug(s"Creating a new film with ${entity.id} id")
          complete(filmsService.createWithoutFilling(entity).map(_.toJson))
        }
        } ~
          extractRequestContext { ctx: RequestContext =>
            fileUpload("csv") {
              case (metadata, byteSource) =>
                LOGGER.debug(s"File ${metadata.fileName} with format ${metadata.contentType}")
                complete(filmsParserService.parseCSVtoFilm(byteSource, ctx))

            }
          } ~
          pathPrefix("help") {
            filmHelpRoute
          }
      } ~
      put {
        pathPrefix("public" / IntNumber) {
          id =>
            LOGGER.debug(s"Making film $id id public")
            complete(filmsService.makePublic(id).map(_.toJson))
        }
      }

  }

  val filmHelpRoute: Route = {

    post {
      entity(as[NewFilmWithFields]) { customer => {
        complete(filmsService.createFilmWithFilling(customer).map(_.toJson))
      }
      }
    }

  }

  val generalFilmsRoute: Route = {
    get {
      pathPrefix("directors") {
        parameter("name") {
          name =>
            complete(filmsService.getFullFilmsByDirector(name).map(_.toJson))
        }
      } ~
        parameter("name", "releaseDate", "directorName") { (name, date, directorName) =>
          LOGGER.debug(s"Searching with params: name $name, date: $date, director name: $directorName")
          complete(filmsService.getFullByDirectorNameDate(directorName, name, date).map(_.toJson))
        } ~ parameter("name", "releaseDate") {
        (name, date) =>
          complete(filmsService.getFullByNameDate(name, date))
      } ~ parameter("name", "directorName") {
        (name, directorName) =>
          complete(filmsService.getFullByDirectorName(directorName, name).map(_.toJson))
      } ~ parameter("releaseDate", "directorName") {
        (date, directorName) =>
          complete(filmsService.getFullByDirectorDate(directorName, date).map(_.toJson))
      } ~ parameter("releaseDate") {
        (date) =>
          complete(filmsService.getFullByDate(date).map(_.toJson))
      } ~ parameter("name") {
        (name) =>
          LOGGER.debug(s"Getting film by name $name")
          complete(filmsService.getFullByName(name).map(_.toJson))
      } ~ pathPrefix("public") {
        complete(filmsService.getAllPublic.map(_.toJson))
      }
    }
  }


}
