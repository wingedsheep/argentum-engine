package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bolt Hound
 * {2}{R}
 * Creature — Elemental Dog
 * 2/2
 * Haste
 * Whenever this creature attacks, other creatures you control get +1/+0 until end of turn.
 */
val BoltHound = card("Bolt Hound") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Dog"
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "Whenever this creature attacks, other creatures you control get +1/+0 until end of turn."
    power = 2
    toughness = 2
    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.modifyStatsForAll(
            power = 1,
            toughness = 0,
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Forrest Imel"
        flavorText = "Its spark is worse than its bite."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f8cf3f9-4e3b-4af2-b5ef-a97eb1d2b674.jpg?1783930695"
    }
}
