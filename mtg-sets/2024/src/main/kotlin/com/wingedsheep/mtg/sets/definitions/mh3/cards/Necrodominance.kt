package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.SkipDrawStep
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Necrodominance
 * {B}{B}{B}
 * Legendary Enchantment
 *
 * Skip your draw step.
 * At the beginning of your end step, you may pay any amount of life. If you do, draw that many cards.
 * Your maximum hand size is five.
 * If a card or token would be put into your graveyard from anywhere, exile it instead.
 *
 * The end-step payment is `Effects.MayPayAnyAmountOfLife`: the amount is chosen at resolution and
 * bound as X, which the draw reads as "that many".
 */
val Necrodominance = card("Necrodominance") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment"
    oracleText = "Skip your draw step.\n" +
        "At the beginning of your end step, you may pay any amount of life. If you do, draw that many cards.\n" +
        "Your maximum hand size is five.\n" +
        "If a card or token would be put into your graveyard from anywhere, exile it instead."

    staticAbility {
        ability = SkipDrawStep
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.MayPayAnyAmountOfLife(Effects.DrawCards(DynamicAmounts.xValue()))
    }

    staticAbility {
        ability = SetMaximumHandSize(amount = DynamicAmounts.fixed(5))
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter(
                    controllerPredicate = ControllerPredicate.OwnedByYou
                ),
                to = Zone.GRAVEYARD
            )
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "102"
        artist = "Robin Olausson"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffc0109c-f939-4424-820e-d6e60cacd794.jpg?1783911278"
        ruling(
            "2024-06-07",
            "If multiple effects modify your hand size, apply them in timestamp order. For example, if you put " +
                "Necrodominance onto the battlefield and then put Spellbook (an artifact that says you have no " +
                "maximum hand size) onto the battlefield, you would have no maximum hand size. However, if those " +
                "permanents entered the battlefield in the opposite order, your maximum hand size would be five."
        )
        ruling(
            "2024-06-07",
            "While Necrodominance is on the battlefield under your control, abilities that trigger whenever a " +
                "permanent you own is put into your graveyard from the battlefield (for example, \"When this " +
                "creature dies…\") won't trigger because cards and tokens are never put into your graveyard."
        )
        ruling(
            "2024-06-07",
            "If a Necrodominance you control is destroyed by a spell you own, Necrodominance will be exiled and " +
                "then the spell will be put into your graveyard."
        )
    }
}
