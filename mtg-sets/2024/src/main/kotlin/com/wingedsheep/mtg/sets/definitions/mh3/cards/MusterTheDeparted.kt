package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Muster the Departed — Modern Horizons 3 #36 (uncommon)
 * {2}{W} · Enchantment
 *
 * When this enchantment enters, create a 1/1 white Spirit creature token with flying.
 * Morbid — At the beginning of your end step, if a creature died this turn, populate.
 *
 * Populate (CR 701.36) chooses — never targets — a creature token you control as the ability
 * resolves and copies it; with none, nothing happens. That is exactly
 * [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens (the executor scopes the
 * choice to the controller's permanents). Morbid is the intervening-if
 * [Conditions.CreatureDiedThisTurn], rechecked on resolution.
 */
val MusterTheDeparted = card("Muster the Departed") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 1/1 white Spirit creature token with flying.\n" +
        "Morbid — At the beginning of your end step, if a creature died this turn, populate. " +
        "(Create a token that's a copy of a creature token you control.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/a/0/a0aa2f5e-9809-4e97-b9b2-9c9a322a8e21.jpg?1783911118"
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
        description = "Morbid — At the beginning of your end step, if a creature died this turn, populate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a37510bb-245c-47bc-bbf3-2b202b67d383.jpg?1783911298"
    }
}
