package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Malcator, Purity Overseer
 * {1}{W}{U}
 * Legendary Creature — Phyrexian Elephant Wizard
 * 1/1
 * When Malcator enters, create a 3/3 colorless Phyrexian Golem artifact creature token.
 * At the beginning of your end step, if three or more artifacts entered the battlefield under
 * your control this turn, create a 3/3 colorless Phyrexian Golem artifact creature token.
 *
 * The end-step check is an intervening-if (CR 603.4) over the turn's entry log, so artifacts that
 * have since left the battlefield still count.
 */
private val PhyrexianGolem = Effects.CreateToken(
    power = 3,
    toughness = 3,
    creatureTypes = setOf("Phyrexian", "Golem"),
    artifactToken = true,
    imageUri = "https://cards.scryfall.io/normal/front/6/3/63ace2fa-8cfb-4641-a05c-d12830378e03.jpg?1783918166"
)

val MalcatorPurityOverseer = card("Malcator, Purity Overseer") {
    manaCost = "{1}{W}{U}"
    typeLine = "Legendary Creature — Phyrexian Elephant Wizard"
    power = 1
    toughness = 1
    oracleText = "When Malcator enters, create a 3/3 colorless Phyrexian Golem artifact creature token.\n" +
        "At the beginning of your end step, if three or more artifacts entered the battlefield under " +
        "your control this turn, create a 3/3 colorless Phyrexian Golem artifact creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = PhyrexianGolem
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.cardTypeEnteredUnderControlThisTurn(CardType.ARTIFACT),
            ComparisonOperator.GTE,
            3
        )
        effect = PhyrexianGolem
        description = "At the beginning of your end step, if three or more artifacts entered the " +
            "battlefield under your control this turn, create a 3/3 colorless Phyrexian Golem " +
            "artifact creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36bda977-e5d1-4813-a5f5-265023965142.jpg?1783918000"
    }
}
