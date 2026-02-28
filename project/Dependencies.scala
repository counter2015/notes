object Versions {
  val scala3 = "3.8.2"
  val circe = "0.14.15"
  val tapir = "1.13.9"
  val munit = "1.2.3"
  val logback = "1.5.32"
  val postgresql = "42.7.10"
}

final case class DependencyCoordinate(
    organization: String,
    artifact: String,
    version: String
)

object Libraries {
  object Circe {
    val core = DependencyCoordinate("io.circe", "circe-core", Versions.circe)
    val generic = DependencyCoordinate("io.circe", "circe-generic", Versions.circe)
  }

  object Tapir {
    val core = DependencyCoordinate("com.softwaremill.sttp.tapir", "tapir-core", Versions.tapir)
    val jsonCirce = DependencyCoordinate("com.softwaremill.sttp.tapir", "tapir-json-circe", Versions.tapir)
    val nettyServerSync = DependencyCoordinate("com.softwaremill.sttp.tapir", "tapir-netty-server-sync", Versions.tapir)
  }

  object Testing {
    val munit = DependencyCoordinate("org.scalameta", "munit", Versions.munit)
  }

  object Logging {
    val logbackClassic = DependencyCoordinate("ch.qos.logback", "logback-classic", Versions.logback)
  }

  object Database {
    val postgresql = DependencyCoordinate("org.postgresql", "postgresql", Versions.postgresql)
  }
}

object ModuleDependencies {
  val sharedCross: Seq[DependencyCoordinate] = Seq(
    Libraries.Circe.core,
    Libraries.Circe.generic
  )

  val backendJvmScala: Seq[DependencyCoordinate] = Seq(
    Libraries.Tapir.core,
    Libraries.Tapir.jsonCirce,
    Libraries.Tapir.nettyServerSync
  )

  val backendJvmJava: Seq[DependencyCoordinate] = Seq(
    Libraries.Database.postgresql
  )

  val frontendJs: Seq[DependencyCoordinate] = Seq.empty
}
