package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flayer Drone
 * {1}{B}{R}
 * Creature — Eldrazi Drone
 * 3/1
 * Devoid (This card has no color.)
 * First strike
 * Whenever another colorless creature you control enters, target opponent loses 1 life.
 *
 * The "another colorless creature you control enters" subject is the Glaring Fleshraker / Eldrazi
 * Mimic shape: `another(Creature.youControl()).enters()` narrowed by a `triggerRestriction` on the
 * entering creature, checked when the event happens (a trigger condition, not an intervening "if").
 */
private val colorless = GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless)

val FlayerDrone = card("Flayer Drone") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "First strike\n" +
        "Whenever another colorless creature you control enters, target opponent loses 1 life."

    keywords(Keyword.DEVOID, Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        triggerRestriction = Conditions.EntityMatches(EffectTarget.TriggeringEntity, colorless)
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(1, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/3931db42-3773-4aae-b6eb-2209e7312f8c.jpg?1783937897"
    }
}
