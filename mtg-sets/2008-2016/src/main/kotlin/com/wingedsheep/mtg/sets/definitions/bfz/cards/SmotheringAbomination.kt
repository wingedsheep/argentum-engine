package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Smothering Abomination
 * {2}{B}{B}
 * Creature — Eldrazi
 * 4/3
 * Devoid (This card has no color.)
 * Flying
 * At the beginning of your upkeep, sacrifice a creature.
 * Whenever you sacrifice a creature, draw a card.
 *
 * The upkeep sacrifice may be Smothering Abomination itself, and the draw trigger sees that sacrifice
 * too (leaves-the-battlefield look-back), so even sacrificing itself draws a card.
 */
val SmotheringAbomination = card("Smothering Abomination") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi"
    power = 4
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "Flying\n" +
        "At the beginning of your upkeep, sacrifice a creature.\n" +
        "Whenever you sacrifice a creature, draw a card."

    keywords(Keyword.DEVOID, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.SacrificeOwn(GameObjectFilter.Creature)
    }

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Creature)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd81d0de-01a3-4f28-8a2b-c2e6f36a03c4.jpg?1783938204"
    }
}
