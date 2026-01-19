package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.sets.BaseCardRegistry
import com.wingedsheep.rulesengine.sets.ecl.cards.AdeptWatershaper
import com.wingedsheep.rulesengine.sets.ecl.cards.ChangelingWayfinder
import com.wingedsheep.rulesengine.sets.ecl.cards.RooftopPercher

object LorwynEclipsedSet : BaseCardRegistry() {
    override val setCode: String = "ECL"
    override val setName: String = "Lorwyn Eclipsed"

    init {
        register(ChangelingWayfinder.definition, ChangelingWayfinder.script)
        register(RooftopPercher.definition, RooftopPercher.script)
        register(AdeptWatershaper.definition, AdeptWatershaper.script) // Added
    }
}
