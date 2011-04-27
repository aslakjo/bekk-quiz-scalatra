
package no.aslakjo.bekkquiz.model;
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Schema}

class Question (
  val alternativ1: String,
  val alternativ2: String,
  val alternativ3: String,
  val alternativ4: String,
  val alternativ5: String,
  val title:String,
  val question:String,
  val explenation:String
) extends KeyedEntity[Long]{
  val id:Long = 0
  def this() = this("","","","","","","","")
  override def toString = "%s %s (%s, %s, %s, %s, %s)".format(
    title, question,
    alternativ1, alternativ2, alternativ3, alternativ4, alternativ5
  )
}


object QuizModel extends Schema {

  val questions = table[Question]("QUESTIONS")

  on(questions)(q => declare(
      q.question    is(dbType("varchar(512)")),
      q.explenation is(dbType("varchar(512)"))
    )
  )

  override def drop = super.drop
 }