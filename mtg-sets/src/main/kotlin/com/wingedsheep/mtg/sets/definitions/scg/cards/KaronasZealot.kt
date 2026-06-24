package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RedirectNextDamageEffect
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Karona's Zealot
 * {4}{W}
 * Creature — Human Cleric
 * 2/5
 * Morph {3}{W}{W}
 * When this creature is turned face up, all damage that would be dealt to it
 * this turn is dealt to target creature instead.
 */
val KaronasZealot = card("Karona's Zealot") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 5
    oracleText = "Morph {3}{W}{W} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, all damage that would be dealt to it this turn is dealt to target creature instead."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val creature = target("target creature", Targets.Creature)
        effect = RedirectNextDamageEffect(
            protectedTargets = listOf(EffectTarget.Self),
            redirectTo = creature,
            amount = null,
            // "ALL damage that would be dealt to it this turn" — a continuous shield that redirects
            // every instance for the rest of the turn, never used up by a single redirection.
            scope = RedirectScope.CONTINUOUS
        )
    }

    morph = "{3}{W}{W}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/914a1200-b77c-4a2c-96c6-7cc624ee9a6a.jpg?1562532125"
    }
}
