package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Orcish Bowmasters
 * {1}{B}
 * Creature — Orc Archer
 * 1/1
 *
 * Flash
 * When this creature enters and whenever an opponent draws a card except the first one they
 * draw in each of their draw steps, this creature deals 1 damage to any target. Then amass Orcs 1.
 *
 * The errata'd Oracle text folds the enters trigger and the opponent-draw trigger into one
 * ability with two trigger conditions. They never fire from the same event, so we model them as
 * two sibling triggered abilities sharing the same effect: deal 1 damage to any target, then
 * amass Orcs 1. The "except the first card they draw in each of their draw steps" clause is the
 * [Triggers.OpponentDrawsExceptFirstEachDrawStep] primitive (CR 504.1 turn-based draw is exempt;
 * every other draw fires once per card).
 */
val OrcishBowmasters = card("Orcish Bowmasters") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Archer"
    power = 1
    toughness = 1
    oracleText = "Flash\nWhen this creature enters and whenever an opponent draws a card except the first one they draw in each of their draw steps, this creature deals 1 damage to any target. Then amass Orcs 1."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", AnyTarget())
        effect = Effects.Composite(
            Effects.DealDamage(1, t),
            Effects.Amass(1, "Orc")
        )
    }

    triggeredAbility {
        trigger = Triggers.OpponentDrawsExceptFirstEachDrawStep
        val t = target("target", AnyTarget())
        effect = Effects.Composite(
            Effects.DealDamage(1, t),
            Effects.Amass(1, "Orc")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Maxim Kostin"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c024bae-5631-4e20-ac69-df392ac9e109.jpg?1745319944"
    }
}
