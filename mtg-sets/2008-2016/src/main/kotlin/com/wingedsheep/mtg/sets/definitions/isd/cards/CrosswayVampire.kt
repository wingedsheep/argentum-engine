package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crossway Vampire
 * {1}{R}{R}
 * Creature — Vampire
 * 3/2
 * When this creature enters, target creature can't block this turn.
 */
val CrosswayVampire = card("Crossway Vampire") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    oracleText = "When this creature enters, target creature can't block this turn."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(creature)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Mark Evans"
        flavorText = "\"Do not flatter Fate. It did not guide you into my clutches tonight. That was all my doing.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e7a137f-e19e-43a6-aab8-02b175c9d626.jpg"
    }
}
