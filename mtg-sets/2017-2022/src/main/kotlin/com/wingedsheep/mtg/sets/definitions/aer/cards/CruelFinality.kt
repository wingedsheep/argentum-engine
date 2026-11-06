package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cruel Finality
 * {2}{B}
 * Instant
 * Target creature gets -2/-2 until end of turn. Scry 1.
 */
val CruelFinality = card("Cruel Finality") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -2/-2 until end of turn. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-2, -2, t) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Svetlin Velinov"
        flavorText = "Bitter the bite of a demon's steel, and deep the abyss it opens."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c22e911-cf40-4ed8-b075-186f2b1393db.jpg?1783936765"
        ruling("2017-02-09", "If the target creature becomes an illegal target, Cruel Finality doesn't resolve and none of its effects happen. You won't scry 1.")
    }
}
