package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawDuringOtherDrawSteps
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Teferi, Who Slows the Sunset
 * {2}{W}{U}
 * Legendary Planeswalker — Teferi
 * Loyalty 4
 *
 * +1: Choose up to one target artifact, up to one target creature, and up to one target land.
 * Untap the chosen permanents you control. Tap the chosen permanents you don't control. You gain
 * 2 life.
 * −2: Look at the top three cards of your library. Put one of them into your hand and the rest on
 * the bottom of your library in any order.
 * −7: You get an emblem with "Untap all permanents you control during each opponent's untap step"
 * and "You draw a card during each opponent's draw step."
 *
 * The +1 gathers whatever was chosen ([CardSource.ChosenTargets], the Urgent Necropsy routing — any
 * of the three may be skipped) and splits it on "you control" at resolution. The emblem's two
 * statics are the emblem's own: Seedborn Muse's untap static and its draw-step sibling, both scoped
 * to opponents' steps; per the ruling, the emblem draw follows the active player's draw with no
 * priority between them.
 */
val TeferiWhoSlowsTheSunset = card("Teferi, Who Slows the Sunset") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Teferi"
    startingLoyalty = 4
    oracleText = "+1: Choose up to one target artifact, up to one target creature, and up to one " +
        "target land. Untap the chosen permanents you control. Tap the chosen permanents you don't " +
        "control. You gain 2 life.\n" +
        "−2: Look at the top three cards of your library. Put one of them into your hand and the " +
        "rest on the bottom of your library in any order.\n" +
        "−7: You get an emblem with \"Untap all permanents you control during each opponent's " +
        "untap step\" and \"You draw a card during each opponent's draw step.\""

    loyaltyAbility(+1) {
        target(TargetFilter.Artifact, optional = true)
        target(TargetFilter.Creature, optional = true)
        target(TargetFilter.Land, optional = true)
        effect = Effects.Pipeline(
            descriptionOverride = "Choose up to one target artifact, up to one target creature, and " +
                "up to one target land. Untap the chosen permanents you control. Tap the chosen " +
                "permanents you don't control. You gain 2 life."
        ) {
            val chosen = gather(CardSource.ChosenTargets)
            val (yours, theirs) = filterSplit(chosen, GameObjectFilter.Permanent.youControl())
            run(Effects.TapCollection(yours, tap = false))
            run(Effects.TapCollection(theirs, tap = true))
            run(Effects.GainLife(2))
        }
    }

    loyaltyAbility(-2) {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 3,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    loyaltyAbility(-7) {
        effect = Effects.CreatePermanentEmblem(
            ownedStaticAbilities = listOf(
                UntapDuringOtherUntapSteps(duringStepsOf = Player.EachOpponent),
                DrawDuringOtherDrawSteps(duringStepsOf = Player.EachOpponent)
            ),
            emblemDescription = "Untap all permanents you control during each opponent's untap step. " +
                "You draw a card during each opponent's draw step."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "245"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad2e18d4-986c-4a44-8f26-1b8689339cfb.jpg?1783925552"
    }
}
