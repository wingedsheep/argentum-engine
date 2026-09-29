package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arachnoid Adaptation
 * {G}
 * Instant
 * Target creature gets +2/+2 and gains reach until end of turn. Untap it.
 */
val ArachnoidAdaptation = card("Arachnoid Adaptation") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 and gains reach until end of turn. Untap it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature) then
            Effects.GrantKeyword(Keyword.REACH, creature) then
            Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Isis"
        flavorText = "\"Deploy the blightwidows. Rankle's pests should make for a delicious meal.\"\n—Ayara, Furnace Queen"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8278069a-bd92-40ee-b7d5-e740dddb00fa.jpg?1783916977"
    }
}
