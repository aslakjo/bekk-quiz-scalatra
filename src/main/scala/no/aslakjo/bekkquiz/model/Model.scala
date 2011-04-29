
package no.aslakjo.bekkquiz.model;
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Schema}
import org.squeryl.dsl.CompositeKey2

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

  lazy val sets = QuizModel.questionsAssosiatedSets.left(this)
}

class SetComposedOfQuestion(
  val questionId : Long,
  val setId : Long
) extends KeyedEntity[CompositeKey2[Long,Long]] {
  def id = compositeKey(questionId, setId)
}

class QuestionSet(
  val name :String,
  val description :String
) extends KeyedEntity[Long]{
  val id :Long = 0
  def this() = this("","")

  lazy val questions = QuizModel.questionsAssosiatedSets.right(this)
}


object QuizModel extends Schema {

  val questions = table[Question]("QUESTIONS")
  val questionSet = table[QuestionSet]

  val questionsAssosiatedSets =
    manyToManyRelation(questions, questionSet)
      .via[SetComposedOfQuestion](
        (q,s, composed) => (q.id === composed.questionId, s.id === composed.setId)
      )

  on(questions)(q => declare(
      q.question    is(dbType("varchar(512)")),
      q.explenation is(dbType("varchar(512)"))
    )
  )

  override def drop = super.drop
 }