package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tibalt, the Fiend-Blooded — Avacyn Restored #161
 * {R}{R} · Legendary Planeswalker — Tibalt · Starting loyalty 2
 *
 * +1: Draw a card, then discard a card at random.
 * −4: Tibalt deals damage equal to the number of cards in target player's hand to that player.
 * −6: Gain control of all creatures until end of turn. Untap them. They gain haste until end of turn.
 *
 * The +1 is Desperate Ravings' draw-then-random-discard, the −4 is Storm Seeker's hand-count damage
 * (counted at resolution, per the ruling), and the −6 is Insurrection's three-pass group sweep —
 * every creature, including ones you already controlled, untaps and gains haste (ruling).
 */
val TibaltTheFiendBlooded = card("Tibalt, the Fiend-Blooded") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Tibalt"
    startingLoyalty = 2
    oracleText = "+1: Draw a card, then discard a card at random.\n" +
        "−4: Tibalt deals damage equal to the number of cards in target player's hand to that player.\n" +
        "−6: Gain control of all creatures until end of turn. Untap them. They gain haste until end of turn."

    // +1: Draw a card, then discard a card at random.
    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1) then Patterns.Hand.discardRandom(1)
        description = "Draw a card, then discard a card at random."
    }

    // −4: Tibalt deals damage equal to the number of cards in target player's hand to that player.
    loyaltyAbility(-4) {
        val player = target(Targets.Player)
        effect = Effects.DealDamage(
            DynamicAmounts.zone(player.asPlayer, Zone.HAND).count(),
            player,
        )
        description = "Tibalt deals damage equal to the number of cards in target player's hand to that player."
    }

    // −6: Gain control of all creatures until end of turn. Untap them. They gain haste until end of turn.
    loyaltyAbility(-6) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.GainControl(EffectTarget.IterationEntity, Duration.EndOfTurn)
        ) then Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.Untap(EffectTarget.IterationEntity)
        ) then Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn)
        )
        description = "Gain control of all creatures until end of turn. Untap them. They gain haste until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "161"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5ddb3936-8f5a-498d-a46c-27eb9546c76c.jpg?1783940675"

        ruling("2012-05-01", "The number of cards in the target player's hand is determined when Tibalt's second ability resolves.")
        ruling("2012-05-01", "When Tibalt's third ability resolves, all creatures will become untapped and gain haste, including ones you controlled before the ability resolved.")
    }
}
