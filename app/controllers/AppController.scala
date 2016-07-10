package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem

@Singleton
class AppController @Inject()(implicit system: ActorSystem) extends Step7Controller {}

