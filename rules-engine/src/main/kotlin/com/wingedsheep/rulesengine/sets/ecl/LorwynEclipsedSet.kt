package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.sets.BaseCardRegistry
import com.wingedsheep.rulesengine.sets.ecl.cards.AdeptWatershaper
import com.wingedsheep.rulesengine.sets.ecl.cards.BarkOfDoran
import com.wingedsheep.rulesengine.sets.ecl.cards.BrigidClachansHeart
import com.wingedsheep.rulesengine.sets.ecl.cards.BurdenedStoneback
import com.wingedsheep.rulesengine.sets.ecl.cards.ChangelingWayfinder
import com.wingedsheep.rulesengine.sets.ecl.cards.RooftopPercher

object LorwynEclipsedSet : BaseCardRegistry() {
    override val setCode: String = "ECL"
    override val setName: String = "Lorwyn Eclipsed"

    init {
        register(ChangelingWayfinder.definition, ChangelingWayfinder.script)
        register(RooftopPercher.definition, RooftopPercher.script)
        register(AdeptWatershaper.definition, AdeptWatershaper.script)
        register(BarkOfDoran.definition, BarkOfDoran.script)
        register(BrigidClachansHeart.definition, BrigidClachansHeart.script)
        // Also register back face script for Brigid
        registerScript(BrigidClachansHeart.backScript)
        register(BurdenedStoneback.definition, BurdenedStoneback.script)
    }
}
