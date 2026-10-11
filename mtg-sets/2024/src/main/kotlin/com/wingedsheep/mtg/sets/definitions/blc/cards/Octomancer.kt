package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.gift
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GiftKind
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Octomancer — Bloomburrow Commander #37
 * {3}{G}{U} · Creature — Frog Druid · 3/3
 *
 * Gift an Octopus (You may promise an opponent a gift as you cast this spell. If you do, when it
 * enters, they create an 8/8 blue Octopus creature token.)
 * At the beginning of each end step, create a token that's a copy of target creature token that
 * entered the battlefield this turn.
 *
 * The target may be any player's creature token — including the Octopus just gifted to an
 * opponent. The copy takes the token's copiable values (as set by the effect that created it, or
 * whatever it is itself copying), not its counters, tapped state or non-copy effects.
 */
val Octomancer = card("Octomancer") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Frog Druid"
    power = 3
    toughness = 3
    oracleText = "Gift an Octopus (You may promise an opponent a gift as you cast this spell. If you do, " +
        "when it enters, they create an 8/8 blue Octopus creature token.)\n" +
        "At the beginning of each end step, create a token that's a copy of target creature token " +
        "that entered the battlefield this turn."

    gift(GiftKind.OCTOPUS)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        val token = target(TargetFilter(GameObjectFilter.Creature.token().enteredThisTurn()))
        effect = Effects.CreateTokenCopyOfTarget(token)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f260197a-0297-4673-96de-dee915d79353.jpg?1783910726"
        ruling(
            "2024-07-26",
            "The token you create copies the original characteristics of the token as stated by the " +
                "effect that created that token (unless that token is copying something else). It doesn't " +
                "copy whether that token is tapped or untapped, whether it has any counters on it or Auras " +
                "and Equipment attached to it, or any non-copy effects that have changed its power, " +
                "toughness, types, color, or so on."
        )
        ruling(
            "2024-07-26",
            "If the copied token is copying something else (for example, if the copied token is one " +
                "previously created by this ability), then the token enters as whatever that token copied."
        )
    }
}
