package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Brazen Collector
 * {1}{R}
 * Creature — Raccoon Rogue
 * 2/1
 *
 * First strike
 * Whenever this creature attacks, add {R}. Until end of turn, you don't lose
 * this mana as steps and phases end.
 *
 * Note: The "don't lose this mana" clause is effectively a no-op in this engine
 * since mana pools are only emptied at end of turn, not between steps/phases.
 */
val BrazenCollector = card("Brazen Collector") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Raccoon Rogue"
    power = 2
    toughness = 1
    oracleText = "First strike\nWhenever this creature attacks, add {R}. Until end of turn, you don't lose this mana as steps and phases end."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.AddMana(Color.RED, 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Aldo Domínguez"
        flavorText = "Risks taken to retrieve a prize only increase its value."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78b55a58-c669-4dc6-aa63-5d9dff52e613.jpg?1721426587"
    }
}
