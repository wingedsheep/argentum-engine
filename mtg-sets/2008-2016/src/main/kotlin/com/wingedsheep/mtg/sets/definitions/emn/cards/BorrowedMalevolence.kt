package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/** Borrowed Malevolence — Eldritch Moon #82. Borrowed Hostility's escalate shape. */
val BorrowedMalevolence = card("Borrowed Malevolence") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Escalate {2} (Pay this cost for each mode chosen beyond the first.)\n" +
        "Choose one or both —\n" +
        "• Target creature gets +1/+1 until end of turn.\n" +
        "• Target creature gets -1/-1 until end of turn."

    spell {
        modal(chooseCount = 2, minChooseCount = 1, additionalManaCostPerExtraMode = "{2}") {
            mode("Target creature gets +1/+1 until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(1, 1, creature)
            }
            mode("Target creature gets -1/-1 until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(-1, -1, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a71f123e-aad9-4f3e-9f43-1d1be359affb.jpg?1783937487"
        ruling("2016-07-13", "If two of the chosen modes of an escalate spell target a creature, you may choose the same creature for each mode's target, or choose different creatures.")
        ruling("2016-07-13", "If one target of an escalate spell becomes illegal, the other targets will still be affected. If all of the targets become illegal, the spell won't resolve.")
    }
}
