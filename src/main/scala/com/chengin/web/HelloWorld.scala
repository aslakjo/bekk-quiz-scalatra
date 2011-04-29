package com.chengin.web

import org.scalatra._
import scalate.ScalateSupport
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.adapters.H2Adapter
import org.squeryl.{Session, SessionFactory}
import no.aslakjo.bekkquiz.model.{QuizModel, QuestionSet, Question}
import org.scalatra.ScalatraKernel._
import QuizModel._

class HelloWorld extends ScalatraServlet with ScalateSupport with Gruppe with Sporsmal with Laere {
  before {
    contentType = "text/html"
    SessionFactory.newSession.bindToCurrentThread
  }

  after {
    Session.currentSession.close
  }

  override def initialize(config: Config) = {
    super.initialize(config)
    Class.forName("org.h2.Driver")
    SessionFactory.concreteFactory = Some(() =>
      Session.create(
        java.sql.DriverManager.getConnection("jdbc:h2:~/test", "sa", ""),
        new H2Adapter
      )
    )

    SessionFactory.newSession.bindToCurrentThread
    try{
      QuizModel.create
    } catch {
      case _  =>
    }
    Session.currentSession.close
  }

  get("/"){
    <p>
      <h1>Dette kan du gjøre</h1>
      <a href="/laere">Lære</a>

      <a href="/sporsmal">Spørsmål</a>
      <a href="/gruppe">Spørsmål grupper</a>
    </p>
  }

  protected def contextPath = request.getContextPath
}

trait Laere extends ScalatraServlet with ScalateSupport{
  def currentSet(id:Int) = from(questionSet)(s => where(s.id === id) select(s)).single


  get("/laere"){
    val allSets = from(questionSet)(s=> select(s)).toList

    templateEngine.layout("/WEB-INF/laere.scaml",
      Map("sporsmalSet" -> allSets)
    )
  }

  get("/laere/gjennom/:id"){
    redirect("/laere/gjennom/%s/sp/0".format(params("id")))
  }

  get("/laere/gjennom/:id/sp/:spId"){
    val set  = currentSet(params("id").toInt)
    val spId = params("spId").toInt
    val question = set.questions.toList.apply(spId)

    templateEngine.layout("/WEB-INF/svare.scaml",
      Map("sporsmal" -> question)
    )
  }

  post("/laere/gjennom/:id/sp/:spId"){
    val set = currentSet(params("id").toInt)
    val questionId = params("spId").toInt
    val nextQuestionId = questionId + 1

    if(nextQuestionId >= set.questions.size){
      redirect("/laere/gjennom/%s/ferdig".format(params("id")))
    }else{
      val question = set.questions.toList.apply(questionId)
      redirect("/laere/gjennom/%s/sp/%s".format(params("id").toInt, params("spId").toInt +1))
    }
  }

  get("/laere/gjennom/:id/ferdig"){
    <h1>Ferdig!</h1>
  }
}

trait Sporsmal extends ScalatraServlet with ScalateSupport {
  def questionById(id:Int) = from(questions)(s => where(s.id == id) select(s)).single

  get("/sporsmal") {
    val allQuestions: List[Question] = from(questions)(s => select(s)).toList

    templateEngine.layout("/WEB-INF/sporsmal.scaml",
      Map("sporsmal" -> allQuestions)
    )

  }

  get("/sporsmal/nytt"){
    templateEngine.layout("/WEB-INF/nytt.scaml",
      Map("sporsmal" -> new Question)
    )
  }

  post("/sporsmal/nytt"){
    transaction {
      val question = new Question(
        params("alt1").trim, params("alt2").trim, params("alt3").trim, params("alt4").trim, params("alt5").trim,
        params("title").trim,
        params("sporsmal").trim,
        params("forklaring").trim
      )
      val newQuestion = questions.insert(question)
    }

    redirect("/sporsmal")
  }

  get("/sporsmal/rediger/:id"){
    val question = questionById(params("id").toInt)

    templateEngine.layout("/WEB-INF/nytt.scaml",
      Map("sporsmal" -> question)
    )
  }

  post("/sporsmal/rediger/:id"){
    val question = questionById(params("id").toInt)

    update(questions)(q =>
      where(q.id == question.id)
      set(
        q.title := params("title"),
        q.question := params("sporsmal"),
        q.explenation := params("forklaring"),
        q.alternativ1 := params("alt1"),
        q.alternativ2 := params("alt2"),
        q.alternativ3 := params("alt3"),
        q.alternativ4 := params("alt4"),
        q.alternativ5 := params("alt5")
      )
    )

    redirect("/sporsmal")
  }
}

trait Gruppe extends ScalatraServlet with ScalateSupport  {
  get("/gruppe"){
    val allGroups: List[QuestionSet] = from(questionSet)(s => select(s)).toList

    templateEngine.layout("/WEB-INF/set.scaml",
      Map("sporsmalSet" -> allGroups)
    )
  }

  get("/gruppe/nytt"){
    val allQuestions = from(questions)(q => select(q)).toList

    templateEngine.layout("/WEB-INF/edit-set.scaml",
      Map("questions" -> allQuestions)
    )
  }

  post("/gruppe/nytt"){
    val questionIds = multiParams("questions").map(_.toLong)
    val questionGroup = new QuestionSet(params("name"), params("desc"))
    questionSet.insert(questionGroup)

    questionIds.foreach(id =>{
      val question = from(questions)(q=> where(q.id === id) select(q)).single
      questionGroup.questions.associate(question)
    })

    redirect("/gruppe")
  }

  get("/gruppe/slett/:id"){
    questionSet.deleteWhere(qs => qs.id === params("id").toInt)

    redirect("/gruppe")
  }
}
