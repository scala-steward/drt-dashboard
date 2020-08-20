package uk.gov.homeoffice.drt.authentication

object Roles {
  val portRoles: Set[Role] = Set(BHX, BRS, EDI, EMA, LGW, GLA, LCY, BFS, LPL, NCL, LHR, LTN, MAN, STN)

  val availableRoles: Set[Role] = Set(
    FixedPointsEdit,
    StaffMovementsEdit,
    StaffMovementsExport,
    StaffEdit,
    ApiView,
    ManageUsers,
    CreateAlerts,
    ApiViewPortCsv,
    FixedPointsEdit,
    FixedPointsView,
    DesksAndQueuesView,
    ArrivalsAndSplitsView,
    ForecastView,
    BorderForceStaff,
    PortOperatorStaff,
    PortFeedUpload,
    ViewConfig,
    TerminalDashboard,
    ArrivalSource,
    ArrivalSimulationUpload,
    Debug) ++ portRoles

  def parse(roleName: String): Option[Role] = availableRoles.find(role => role.name.toLowerCase == roleName.toLowerCase)

  sealed trait Role {
    val name: String
  }

  case object NoOpRole extends Role {
    override val name: String = "noop"
  }

  case object StaffEdit extends Role {
    override val name: String = "staff:edit"
  }

  case object StaffMovementsEdit extends Role {
    override val name: String = "staff-movements:edit"
  }

  case object StaffMovementsExport extends Role {
    override val name: String = "staff-movements:export"
  }

  case object TerminalDashboard extends Role {
    override val name: String = "terminal-dashboard"
  }

  case object ArrivalSource extends Role {
    override val name: String = "arrival-source"
  }

  case object ApiView extends Role {
    override val name: String = "api:view"
  }

  case object ApiViewPortCsv extends Role {
    override val name: String = "api:view-port-arrivals"
  }

  case object TestAccess extends Role {
    override val name: String = "test"
  }

  case object Test2Access extends Role {
    override val name: String = "test2"
  }

  case object ManageUsers extends Role {
    override val name: String = "manage-users"
  }

  sealed trait PortAccess extends Role

  case object BHX extends PortAccess {
    override val name: String = "BHX"
  }

  case object BRS extends PortAccess {
    override val name: String = "BRS"
  }

  case object EDI extends PortAccess {
    override val name: String = "EDI"
  }

  case object EMA extends PortAccess {
    override val name: String = "EMA"
  }

  case object LGW extends PortAccess {
    override val name: String = "LGW"
  }

  case object GLA extends PortAccess {
    override val name: String = "GLA"
  }

  case object LCY extends PortAccess {
    override val name: String = "LCY"
  }

  case object BFS extends PortAccess {
    override val name: String = "BFS"
  }

  case object LPL extends PortAccess {
    override val name: String = "LPL"
  }

  case object NCL extends PortAccess {
    override val name: String = "NCL"
  }

  case object LHR extends PortAccess {
    override val name: String = "LHR"
  }

  case object LTN extends PortAccess {
    override val name: String = "LTN"
  }

  case object MAN extends PortAccess {
    override val name: String = "MAN"
  }

  case object STN extends PortAccess {
    override val name: String = "STN"
  }

  case object CreateAlerts extends Role {
    override val name: String = "create-alerts"
  }

  case object ViewConfig extends Role {
    override val name: String = "view-config"
  }

  case object FixedPointsEdit extends Role {
    override val name: String = "fixed-points:edit"
  }

  case object FixedPointsView extends Role {
    override val name: String = "fixed-points:view"
  }

  case object DesksAndQueuesView extends Role {
    override val name: String = "desks-and-queues:view"
  }

  case object ArrivalsAndSplitsView extends Role {
    override val name: String = "arrivals-and-splits:view"
  }

  case object ForecastView extends Role {
    override val name: String = "forecast:view"
  }

  sealed trait Staff extends Role

  case object BorderForceStaff extends Staff {
    override val name: String = "border-force-staff"
  }

  case object PortOperatorStaff extends Staff {
    override val name: String = "port-operator-staff"
  }

  case object PortFeedUpload extends Role {
    override val name: String = "port-feed-upload"
  }

  case object ArrivalSimulationUpload extends Role {
    override val name: String = "arrival-simulation-upload"
  }

  case object Debug extends Role {
    override val name: String = "debug"
  }

}
