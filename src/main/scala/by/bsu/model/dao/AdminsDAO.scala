package by.bsu.model.dao

import by.bsu.model.Db
import by.bsu.model.repository.{Admin, AdminsTable}
import org.apache.log4j.Logger
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.language.postfixOps

class AdminsDAO(val config: DatabaseConfig[JdbcProfile])
  extends Db with AdminsTable {

  import config.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  val LOGGER = Logger.getLogger(this.getClass.getName)

  def insertUniq(admin: Admin): Future[Option[Admin]] = {
    LOGGER.debug(s"Inserting admin ${admin.username}")
    db.run(createQuery(admin).asTry).map(_.toOption)
  }

  def insert(admin: Admin): Future[Admin] = {
    db.run(admins returning admins.map(_.admin_id) += admin)
      .map(id => admin.copy(id = Option(id)))
  }

  private def createQuery(entity: Admin): DBIOAction[Admin, NoStream, Effect.Read with Effect.Write with Effect.Transactional] = {
    (for {
      existing <- admins.filter(_.username === entity.username).result //Check, if entity exists
      data <- if (existing.isEmpty)
        (admins returning admins) += entity
      else {
        throw new Exception(s"Create failed: entity already exists")
      }
    } yield (data)).transactionally

  }

  def getPassword(username: String): Future[Admin] = {
    db.run(admins.filter(data => (data.username === username)).result.head)
  }


  def update(id: Int, actor: Admin): Future[Int] = {
    LOGGER.debug(s"Updating admin $id id")
    db.run(admins.filter(_.admin_id === id).map(customer => (customer.username))
      .update(actor.username))
  }

  def findAll(): Future[Seq[Admin]] = db.run(admins.result)

  def deleteById(id: Int): Future[Boolean] = {
    db.run(admins.filter(_.admin_id === id).delete) map {
      _ > 0
    }
  }

  def findById(id: Int): Future[Option[Admin]] = {
    db.run(admins.filter(_.admin_id === id).result.headOption)
  }

  def findByName(name: String): Future[Option[Admin]] = {
    db.run(admins.filter(_.username === name).result.headOption)
  }


  def deleteAll(): Future[Int] = {
    db.run(admins.delete)
  }
}