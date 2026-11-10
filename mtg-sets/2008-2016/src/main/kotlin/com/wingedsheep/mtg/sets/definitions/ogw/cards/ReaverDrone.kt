package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reaver Drone
 * {B}
 * Creature — Eldrazi Drone
 * 2/1
 * Devoid (This card has no color.)
 * At the beginning of your upkeep, you lose 1 life unless you control another colorless creature.
 *
 * "Unless" is checked as the ability resolves (not an intervening "if"), so the trigger always
 * goes on the stack and the colorless-creature check happens on resolution.
 */
val ReaverDrone = card("Reaver Drone") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "At the beginning of your upkeep, you lose 1 life unless you control another colorless creature."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless),
                negate = true,
                excludeSelf = true
            ),
            then = Effects.LoseLife(1, EffectTarget.Controller),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Chris Rallis"
        flavorText = "Kozilek's brood infiltrates and deceives. The spawn of Ulamog mindlessly consume."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/162f3fb8-75b7-4cd7-9d6c-27dd76197aa5.jpg?1783937914"
        ruling(
            "2016-01-22",
            "Whether you control another colorless creature is checked as the ability resolves. This check will happen even if Reaver Drone leaves the battlefield in response to the ability."
        )
    }
}
