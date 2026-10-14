package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Armory Paladin
 * {1}{R}{W}
 * Creature — Human Knight
 * 3/3
 *
 * Trample
 * Whenever you cast an Aura or Equipment spell, exile the top card of your library. You may play
 * that card until the end of your next turn.
 *
 * The impulse recipe ([Patterns.Exile.impulse]) with the "until the end of your next turn" window:
 * [MayPlayExpiry.UntilControllerStep] at cleanup with `includeCurrentTurn = false`.
 */
val ArmoryPaladin = card("Armory Paladin") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Knight"
    oracleText = "Trample\n" +
        "Whenever you cast an Aura or Equipment spell, exile the top card of your library. " +
        "You may play that card until the end of your next turn."
    power = 3
    toughness = 3

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Any.withSubtype(Subtype.AURA) or GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT)
        )
        effect = Patterns.Exile.impulse(
            count = 1,
            expiry = MayPlayExpiry.UntilControllerStep(Step.CLEANUP, includeCurrentTurn = false)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Javier Charro"
        flavorText = "\"Ad victoriam!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e5ffa95-5c5f-449b-820a-7d5bc1688d29.jpg?1783912393"
    }
}
