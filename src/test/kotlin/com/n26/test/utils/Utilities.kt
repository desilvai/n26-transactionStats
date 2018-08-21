import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP


/**
 * Rounds up a BigDecimal.  This is a more readable shorthand.
 */
fun BigDecimal.roundUp(numberOfPlaces: Int): BigDecimal
        = this.setScale(numberOfPlaces, HALF_UP)