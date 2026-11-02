package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dovin, Architect of Law — Ravnica Allegiance #265 (planeswalker deck)
 * {4}{W}{U} Legendary Planeswalker — Dovin, loyalty 5
 *
 * +1: You gain 2 life and draw a card.
 * −1: Tap target creature. It doesn't untap during its controller's next untap step.
 * −9: Tap all permanents target opponent controls. That player skips their next untap step.
 *
 * The −1 is Frost Trickster's shape (an already-tapped creature is a legal target and still
 * misses its next untap, per ruling); the −9 gathers the target opponent's permanents at
 * resolution, taps them, then skips that player's next untap step.
 */
val DovinArchitectOfLaw = card("Dovin, Architect of Law") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Dovin"
    startingLoyalty = 5
    oracleText = "+1: You gain 2 life and draw a card.\n" +
        "−1: Tap target creature. It doesn't untap during its controller's next untap step.\n" +
        "−9: Tap all permanents target opponent controls. That player skips their next untap step."

    loyaltyAbility(+1) {
        effect = Effects.GainLife(2) then Effects.DrawCards(1)
    }

    loyaltyAbility(-1) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
    }

    loyaltyAbility(-9) {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val permanents = gather(CardSource.ControlledPermanents(Player.TargetPlayer, GameObjectFilter.Permanent))
            run(Effects.TapCollection(permanents))
        } then Effects.SkipNextStepOrPhase(TurnPart.UNTAP_STEP, opponent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "265"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0c7cffe-8751-4da7-8c1c-59f472ef3735.jpg?1783933609"
        ruling("2019-01-25", "Dovin's second ability can target a creature that's already tapped. That creature won't untap during its controller's next untap step.")
        ruling("2019-01-25", "In a Two-Headed Giant game, if a player is instructed to skip their next untap step, the entire team skips that step.")
    }
}
