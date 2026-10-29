package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gale Swooper — Core Set 2021 #20 (canonical printing)
 * {3}{W} · Creature — Griffin · 3/2
 *
 * Flying
 * When this creature enters, target creature gains flying until end of turn.
 */
val GaleSwooper = card("Gale Swooper") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Griffin"
    power = 3
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, target creature gains flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Johann Bodin"
        flavorText = "\"Be glad the griffin set you back on your feet when it was done with you. My goat wasn't so lucky.\"\n—Arram, goatherd"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2e3b99c-e48e-4f4d-ba7a-e9218137b432.jpg?1783930741"
    }
}
