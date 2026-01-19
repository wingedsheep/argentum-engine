package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.sets.BaseCardRegistry
import com.wingedsheep.rulesengine.sets.ecl.cards.ChangelingWayfinder
import com.wingedsheep.rulesengine.sets.ecl.cards.RooftopPercher

/**
 * The Lorwyn Eclipsed set (ECL).
 * A custom set featuring tribal mechanics and Changelings.
 */
object LorwynEclipsedSet : BaseCardRegistry() {
    override val setCode: String = "ECL"
    override val setName: String = "Lorwyn Eclipsed"

    init {
        register(ChangelingWayfinder.definition, ChangelingWayfinder.script)
        register(RooftopPercher.definition, RooftopPercher.script)
    }
}
