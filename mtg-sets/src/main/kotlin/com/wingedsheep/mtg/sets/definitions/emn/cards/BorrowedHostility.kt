package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/** Borrowed Hostility — Eldritch Moon #121. */
val BorrowedHostility = card("Borrowed Hostility") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Escalate {3} (Pay this cost for each mode chosen beyond the first.)\n" +
        "Choose one or both —\n" +
        "• Target creature gets +3/+0 until end of turn.\n" +
        "• Target creature gains first strike until end of turn."

    spell {
        modal(chooseCount = 2, minChooseCount = 1, additionalManaCostPerExtraMode = "{3}") {
            mode("Target creature gets +3/+0 until end of turn.") {
                val creature = target("power target", TargetCreature())
                effect = Effects.ModifyStats(3, 0, creature)
            }
            mode("Target creature gains first strike until end of turn.") {
                val creature = target("first strike target", TargetCreature())
                effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd91a194-6043-4c2d-afc8-427c38996ef4.jpg?1783937466"
        ruling("2016-07-13", "If two chosen modes target a creature, you may choose the same creature for both targets or different creatures.")
        ruling("2016-07-13", "If one target becomes illegal, the other target is still affected. If all targets become illegal, the spell doesn't resolve.")
    }
}
