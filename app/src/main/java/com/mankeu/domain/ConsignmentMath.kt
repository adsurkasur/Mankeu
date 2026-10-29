package com.mankeu.domain

import java.math.BigDecimal

data class ConsignmentMove(val contactId: String, val productId: String, val baseQuantity: BigDecimal, val kind: Kind) {
    enum class Kind { SENT, SOLD, RETURNED }
}

object ConsignmentMath {
    /** Base units still at each (contact, product): sent - sold - returned; never-below-zero is enforced by the repository. */
    fun balances(moves: List<ConsignmentMove>): Map<Pair<String, String>, BigDecimal> =
        moves.groupBy { it.contactId to it.productId }.mapValues { (_, list) ->
            list.fold(BigDecimal.ZERO) { sum, m ->
                if (m.kind == ConsignmentMove.Kind.SENT) sum + m.baseQuantity else sum - m.baseQuantity
            }
        }
}
