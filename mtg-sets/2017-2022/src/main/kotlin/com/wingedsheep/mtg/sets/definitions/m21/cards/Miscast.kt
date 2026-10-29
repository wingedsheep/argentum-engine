package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Miscast
 * {U}
 * Instant
 * Counter target instant or sorcery spell unless its controller pays {3}.
 *
 * Disrupt's shape with a {3} tax and no cantrip.
 */
val Miscast = card("Miscast") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target instant or sorcery spell unless its controller pays {3}."

    spell {
        target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CounterUnlessPays("{3}")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Steve Argyle"
        flavorText = "\"Next time, try something more appropriate for your skill level.\"\n—Barrin, Tolarian archmage"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/033afbd5-9937-4957-98ba-48e469a490bb.jpg?1783930726"
    }
}
