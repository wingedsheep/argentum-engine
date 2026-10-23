package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Humiliate — Strixhaven: School of Mages #193
 * {W}{B} · Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards
 * that card. Put a +1/+1 counter on a creature you control.
 *
 * The discard half is Duress's pipeline with a plain nonland filter. The counter half doesn't
 * target: the creature is chosen on resolution, after the hand is revealed (2021-04-16 ruling),
 * so it's a non-targeting `selectTarget` (Kayla's Command's shape) — and the spell can be cast
 * with no creatures at all.
 */
val Humiliate = card("Humiliate") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player " +
        "discards that card. Put a +1/+1 counter on a creature you control."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
            val chosen = selectTarget(
                TargetObject(filter = TargetFilter.CreatureYouControl),
                nonTargeting = true,
                prompt = "Choose a creature you control to get a +1/+1 counter"
            )
            run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, chosen.asTarget))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Randy Vargas"
        flavorText = "\"Aw, what's that? No comeback?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc3ae8cb-fbdb-45a8-83c2-cbf4aff01f90.jpg?1783927311"
        ruling("2021-04-16", "You may cast Humiliate even if you control no creatures.")
        ruling(
            "2021-04-16",
            "You choose which creature to put the +1/+1 counter on as Humiliate is resolving, after their " +
                "hand has been revealed.",
        )
    }
}
