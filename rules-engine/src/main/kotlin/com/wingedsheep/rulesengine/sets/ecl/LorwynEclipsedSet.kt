package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.sets.BaseCardRegistry
import com.wingedsheep.rulesengine.sets.ecl.cards.ChangelingWayfinder

/**
 * The Lorwyn Eclipsed set (ECL).
 * A custom set featuring tribal mechanics and Changelings.
 */
object LorwynEclipsedSet : BaseCardRegistry() {
    override val setCode: String = "ECL"
    override val setName: String = "Lorwyn Eclipsed"

    init {
        // Register cards
        register(ChangelingWayfinder.definition, ChangelingWayfinder.script)

        // Future cards will be added here
        // register(ChangelingTitan.definition, ChangelingTitan.script)
    }
}
