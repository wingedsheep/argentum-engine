package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoyaltyActivationPermission
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Teferi, Master of Time
 * {2}{U}{U}
 * Legendary Planeswalker — Teferi
 * You may activate loyalty abilities of Teferi on any player's turn any time you could cast an instant.
 * +1: Draw a card, then discard a card.
 * −3: Target creature you don't control phases out.
 * −10: Take two extra turns after this one.
 *
 * The first line is a self-scoped instant-speed [LoyaltyActivationPermission]: it lifts only the
 * timing half of CR 606.3, so Teferi still activates one loyalty ability each turn — which, with
 * instant timing, means once on every player's turn.
 */
val TeferiMasterOfTime = card("Teferi, Master of Time") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Teferi"
    startingLoyalty = 3
    oracleText = "You may activate loyalty abilities of Teferi on any player's turn any time you could cast an instant.\n" +
        "+1: Draw a card, then discard a card.\n" +
        "−3: Target creature you don't control phases out. (Treat it and anything attached to it as though they don't exist until its controller's next turn.)\n" +
        "−10: Take two extra turns after this one."

    staticAbility {
        ability = LoyaltyActivationPermission(GameObjectFilter.Any.sourceItself(), instantSpeed = true)
    }

    loyaltyAbility(+1) {
        effect = Patterns.Hand.loot()
    }

    loyaltyAbility(-3) {
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.PhaseOut(creature)
    }

    loyaltyAbility(-10) {
        effect = Effects.TakeExtraTurn() then Effects.TakeExtraTurn()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "75"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c0c61e3-9f3d-4e7f-9046-0ea336dd8a2d.jpg?1783930718"
        ruling("2020-06-23", "Teferi's first ability doesn't change how many of his abilities you can activate during each turn.")
        ruling("2020-06-23", "You draw a card and discard a card all while Teferi's first loyalty ability is resolving. Nothing can happen between the two, and no player may choose to take actions.")
        ruling("2020-06-23", "While a permanent is phased out, it's treated as though it doesn't exist. It can't be the target of spells or abilities, its static abilities have no effect on the game, its triggered abilities can't trigger, it can't attack or block, and so on.")
        ruling("2020-06-23", "An attacking creature that phases out is removed from combat.")
        ruling("2020-06-23", "Phasing out doesn't cause any \"leaves the battlefield\" abilities to trigger. Similarly, phasing in won't cause any \"enters the battlefield\" abilities to trigger.")
    }
}
