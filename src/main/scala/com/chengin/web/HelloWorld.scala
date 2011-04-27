package com.chengin.web

import org.scalatra._
import scalate.ScalateSupport
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.adapters.H2Adapter
import org.squeryl.{Session, SessionFactory}
import no.aslakjo.bekkquiz.model.{Question, QuizModel}
import QuizModel._

class HelloWorld extends ScalatraServlet with ScalateSupport {


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
      <a href="/sporsmal">Spørsmål</a>
    </p>
  }

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

  def questionById(id:Int) = from(questions)(s => where(s.id == id) select(s)).single

  protected def contextPath = request.getContextPath
}
