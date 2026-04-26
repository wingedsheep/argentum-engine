package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GrantChosenColor
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Puca's Eye
 * {2}
 * Artifact
 *
 * When this artifact enters, draw a card, then choose a color. This artifact becomes the chosen color.
 * {3}, {T}: Draw a card. Activate only if there are five colors among permanents you control.
 */
val PucasEye = card("Puca's Eye") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw a card, then choose a color. This artifact becomes the chosen color.\n" +
        "{3}, {T}: Draw a card. Activate only if there are five colors among permanents you control."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.ChooseColorForTarget(EffectTarget.Self)
        )
    }

    staticAbility {
        ability = GrantChosenColor(StaticTarget.SourceCreature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.DrawCards(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Compare(
                    DynamicAmounts.colorsAmongPermanents(),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(5)
                )
            )
        )
        description = "{3}, {T}: Draw a card. Activate only if there are five colors among permanents you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "259"
        artist = "Dan Frazier"
        flavorText = "\"I swear it watches me as I walk by.\"\n—Toch, Ballynock sage"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3a5ca02-3829-4c42-b0dc-98b660f8a8f0.jpg?1767872205"
    }
}
