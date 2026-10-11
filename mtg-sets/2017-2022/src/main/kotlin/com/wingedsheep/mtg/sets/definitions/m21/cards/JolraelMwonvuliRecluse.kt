package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jolrael, Mwonvuli Recluse
 * {1}{G}
 * Legendary Creature — Human Druid
 * 1/2
 *
 * Whenever you draw your second card each turn, create a 2/2 green Cat creature token.
 * {4}{G}{G}: Until end of turn, creatures you control have base power and toughness X/X, where X
 * is the number of cards in your hand.
 *
 * The trigger is `Triggers.you.drawsNth(2)` — "your second card each turn" (CR 121.2), so a
 * single draw-two crosses the threshold and fires it exactly once.
 *
 * The activated ability is Mirror Entity's shape: the group is gathered at resolution and each
 * creature's base P/T is set in layer 7b. `SetBasePowerAndToughness` without
 * `reevaluateContinuously` freezes the hand count at resolution, per the ruling that X doesn't
 * change later in the turn as your hand size changes.
 */
val JolraelMwonvuliRecluse = card("Jolrael, Mwonvuli Recluse") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Druid"
    power = 1
    toughness = 2
    oracleText = "Whenever you draw your second card each turn, create a 2/2 green Cat creature token.\n" +
        "{4}{G}{G}: Until end of turn, creatures you control have base power and toughness X/X, " +
        "where X is the number of cards in your hand."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Cat"),
            imageUri = "https://cards.scryfall.io/normal/front/f/6/f6541414-c506-449d-b3a8-79f47be531a9.jpg?1783930590"
        )
    }

    activatedAbility {
        cost = Costs.Mana("{4}{G}{G}")
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.SetBasePowerAndToughness(
                power = DynamicAmounts.cardsInYourHand(),
                toughness = DynamicAmounts.cardsInYourHand(),
                target = EffectTarget.IterationEntity,
                duration = Duration.EndOfTurn
            )
        )
        description = "Until end of turn, creatures you control have base power and toughness X/X, " +
            "where X is the number of cards in your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Izzy"
        flavorText = "\"Humans bring nothing but suffering and deceit. They are not welcome in my forest.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dcdcfce-3f32-48f9-83f8-87b9ccbf92e3.jpg?1783930672"

        ruling(
            "2022-12-08",
            "The triggered ability can trigger only once each turn. It doesn't matter if Jolrael was on " +
                "the battlefield when the first card was drawn: if it's not on the battlefield when the " +
                "second card is drawn, the ability can't trigger at all that turn."
        )
        ruling(
            "2022-12-08",
            "Jolrael's last ability overwrites all previous effects that set the affected creatures' " +
                "power and/or toughness to specific values. If other effects that set these " +
                "characteristics to specific values start to apply after Jolrael's ability resolves, " +
                "they will overwrite that part of the effect."
        )
        ruling(
            "2022-12-08",
            "The value of X is determined only as Jolrael's last ability resolves. Once that happens, " +
                "the value of X won't change later in the turn even if the number of cards in your hand " +
                "changes."
        )
    }
}
