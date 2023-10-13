package portfolzio.util

object RomanNumerals {
  def digitToRoman(digit: String): Option[String] = {
    digit match {
      case "1" => Some("I")
      case "2" => Some("II")
      case "3" => Some("III")
      case "4" => Some("IV")
      case "5" => Some("V")
      case "6" => Some("VI")
      case "7" => Some("VII")
      case "8" => Some("VIII")
      case "9" => Some("IX")
      case _   => None
    }
  }
}
