package by.bsu.model.repository

import by.bsu.model.Db

case class ActorFilm(actorFilmId: Option[Int], actorId: Int, filmId: Int)

trait ActorsFilmsTable extends ActorsTable with FilmsTable {
  this: Db =>

  import config.driver.api._

  class ActorsFilms(tag: Tag) extends Table[ActorFilm](tag, "actors_films") {

    def actor_film_id = column[Option[Int]]("actor_film_id", O.PrimaryKey, O.AutoInc)

    def actor_id = column[Int]("actor_id")

    def film_id = column[Int]("film_id")

    def fk_actor_id = foreignKey("fk_actor_id", actor_id, actors)(_.actor_id)

    def fk_film_id = foreignKey("fk_film_id", film_id, films)(_.filmId)

    def * = (actor_film_id, actor_id, film_id) <> (ActorFilm.tupled, ActorFilm.unapply)
  }

  val actorsFilms = TableQuery[ActorsFilms]
}
