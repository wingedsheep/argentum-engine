package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heir of Falkenrath // Heir to the Night (Shadows over Innistrad #116)
 * {1}{B}
 * Creature — Vampire 2/1 // Creature — Vampire Berserker 3/2
 *
 * Front — "Discard a card: Transform this creature. Activate only once each turn."
 * Back  — "Flying"
 *
 * The back face has no mana cost, so its black comes from a color indicator (CR 204).
 */

private val HeirOfFalkenrathFront = card("Heir of Falkenrath") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 1
    oracleText = "Discard a card: Transform this creature. Activate only once each turn."

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.Transform(EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "Discard a card: Transform this creature. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Jason Rainville"
        flavorText = "Among vampires, there has long been the belief that the urge to feed must be " +
            "balanced by the poise of nobility."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf2ad186-4d85-4628-ae84-3c58c88d34cb.jpg?1783937783"
    }
}

private val HeirToTheNight = card("Heir to the Night") {
    manaCost = ""
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Vampire Berserker"
    power = 3
    toughness = 2
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Jason Rainville"
        flavorText = "The Falkenrath have relinquished all pretense at nobility, losing themselves " +
            "completely to their all-consuming thirst."
        imageUri = "https://cards.scryfall.io/normal/back/c/f/cf2ad186-4d85-4628-ae84-3c58c88d34cb.jpg?1783937783"
    }
}

val HeirOfFalkenrath: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HeirOfFalkenrathFront,
    backFace = HeirToTheNight,
)
