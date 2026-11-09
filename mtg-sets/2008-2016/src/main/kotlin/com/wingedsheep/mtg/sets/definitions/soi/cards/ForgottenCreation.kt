package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion

/**
 * Forgotten Creation (Shadows over Innistrad #63)
 * {3}{U}
 * Creature — Zombie Horror
 * 3/3
 * Skulk
 * At the beginning of your upkeep, you may discard all the cards in your hand. If you do, draw
 * that many cards.
 *
 * The "you may … If you do" pair is the [Effects.May] + [Effects.IfYouDo] shape Vaultguard Trooper
 * and Byway Barterer use: discarding your hand always succeeds, even with zero cards in it, so the
 * gate uses [SuccessCriterion.Always]. "That many" is the size of the hand gathered at resolution —
 * [Patterns.Hand.discardedHand]'s count (Soratami Seer's handle), which the gate carries from the
 * action into its `then`. An empty hand discards nothing and draws nothing.
 */
val ForgottenCreation = card("Forgotten Creation") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)\n" +
        "At the beginning of your upkeep, you may discard all the cards in your hand. If you do, " +
        "draw that many cards."
    power = 3
    toughness = 3

    keywords(Keyword.SKULK)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            Effects.IfYouDo(
                action = Patterns.Hand.discardHand(),
                then = Effects.DrawCards(Patterns.Hand.discardedHand.count),
                // Discarding your hand can be done with no cards in it.
                successCriterion = SuccessCriterion.Always,
            )
        )
        description = "At the beginning of your upkeep, you may discard all the cards in your hand. " +
            "If you do, draw that many cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Izzy"
        flavorText = "\"My earliest work escapes me.\"\n—Stitcher Geralf"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27f82ee6-b8cd-4dce-b213-3910302f30e9.jpg?1783937798"
        ruling(
            "2016-04-08",
            "The upkeep step is before the draw step, after the untap step. If you have no cards in " +
                "hand, you can't draw for the turn and then discard that card to draw a new one because " +
                "Forgotten Creation's last ability will already have triggered and resolved."
        )
        ruling(
            "2016-04-08",
            "Skulk matters only as blockers are chosen. Modifying either creature's power after " +
                "blockers are chosen won't cause the attacking creature to become unblocked."
        )
        ruling(
            "2016-04-08",
            "If you cause a creature to have 0 power or less, use the actual value (which may be " +
                "negative) to determine whether it can block or be blocked. A creature with skulk and 0 " +
                "or less power most likely won't be blocked, but it won't deal combat damage and won't " +
                "trigger any abilities that trigger when combat damage is dealt."
        )
    }
}
