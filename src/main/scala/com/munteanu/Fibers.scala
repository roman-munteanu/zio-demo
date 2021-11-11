package com.munteanu

import zio.{Exit, ExitCode, UIO, URIO, ZIO}
import zio.duration._

object Fibers extends zio.App {

  // ZIO[R, E, A]
  val example: UIO[Int] = ZIO.succeed(37)

  // Prepare a song routine
  val drumMachineTrack: UIO[String] = ZIO.succeed("Recording Drums")
  val rhythmTrack: UIO[String] = ZIO.succeed("Recording Rhythm Guitar")
  val vocalsTrack: UIO[String] = ZIO.succeed("Recording Vocals")

  def printThread = s"[${Thread.currentThread().getName}]"

  def synchronousRoutine() = for {
    _ <- drumMachineTrack.debug(printThread)
    _ <- rhythmTrack.debug(printThread)
    _ <- vocalsTrack.debug(printThread)
  } yield ()

  // Fiber[E, A]

  def concurrentDrumsAndRhythm() = for {
    _ <- drumMachineTrack.debug(printThread).fork
    _ <- rhythmTrack.debug(printThread)
    _ <- vocalsTrack.debug(printThread)
  } yield ()


  def concurrentRoutine() = for {
    drumMachineFiber <- drumMachineTrack.debug(printThread).fork
    rhythmFiber <- rhythmTrack.debug(printThread).fork
    zippedFiber = drumMachineFiber.zip(rhythmFiber)
    melody <- zippedFiber.join.debug(printThread)
    _ <- ZIO.succeed(s"$melody prepared").debug(printThread) *> vocalsTrack.debug(printThread) // *> means 'and then'
  } yield ()

  // interrupt example
  val humanDrummer = ZIO.succeed("Real Drummer!")
  val drumMachineWithTime = drumMachineTrack.debug(printThread) *> ZIO.sleep(5.seconds) *> ZIO.succeed("Drum track prepared.")

  def concurrentRoutineWithInterrupt() = for {
    _ <- rhythmTrack.debug(printThread)
    drumMachineFiber <- drumMachineWithTime.fork
    _ <- humanDrummer.debug(printThread).fork *> ZIO.sleep(2.seconds) *> drumMachineFiber.interrupt.debug(printThread)
    _ <- ZIO.succeed("Gonna record with a real drummer!").debug(printThread)
  } yield ()

  // uninterruptible example
  val vocalsTrackWithTime = vocalsTrack.debug(printThread) *> ZIO.sleep(5.seconds) *> ZIO.succeed("My vocals recorded.")
  val awfulVocalist = ZIO.succeed("Ghm-grrr.")

  def concurrentRoutineUninterruptible() = for {
    _ <- drumMachineTrack.debug(printThread)
    _ <- rhythmTrack.debug(printThread)
    vocalsFiber <- vocalsTrackWithTime.debug(printThread).fork.uninterruptible
    song <- awfulVocalist.debug(printThread).fork *> vocalsFiber.interrupt.debug(printThread)
    _ <- song match {
      case Exit.Success(value) =>
        ZIO.succeed("Sorry, gonna sing it myself").debug(printThread)
      case _ =>
        ZIO.succeed("Recorded with awful vocals")
    }
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
//    synchronousRoutine().exitCode
//    concurrentDrumsAndRhythm().exitCode
//    concurrentRoutine().exitCode
//    concurrentRoutineWithInterrupt().exitCode
    concurrentRoutineUninterruptible().exitCode
}
