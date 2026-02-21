package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PreventCombatDamageFromEffect

/**
 * Frontline Strategist
 * {W}
 * Creature — Human Soldier
 * 1/1
 * Morph {W}
 * When Frontline Strategist is turned face up, prevent all combat damage
 * non-Soldier creatures would deal this turn.
 */
val FrontlineStrategist = card("Frontline Strategist") {
    manaCost = "{W}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Morph {W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Frontline Strategist is turned face up, prevent all combat damage non-Soldier creatures would deal this turn."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = PreventCombatDamageFromEffect(
            source = Filters.Group.creatures { notSubtype(Subtype("Soldier")) },
            duration = Duration.EndOfTurn
        )
    }

    morph = "{W}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c43fac2-62fb-4924-848d-a8d739773d6e.jpg?1562526115"
    }
}
