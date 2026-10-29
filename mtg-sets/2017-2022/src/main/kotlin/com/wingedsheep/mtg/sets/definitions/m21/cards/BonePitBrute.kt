package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bone Pit Brute
 * {4}{R}{R}
 * Creature — Cyclops
 * 4/5
 *
 * Menace
 * When this creature enters, target creature gets +4/+0 until end of turn.
 */
val BonePitBrute = card("Bone Pit Brute") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cyclops"
    power = 4
    toughness = 5
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature enters, target creature gets +4/+0 until end of turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(4, 0, creature)
        description = "When this creature enters, target creature gets +4/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Victor Adame Minguez"
        flavorText = "Welcome to the club."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/6075e0a3-a0ab-4a11-8ad2-7dabb071d309.jpg?1783930695"
    }
}
