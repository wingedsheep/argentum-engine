package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vodalian War Machine
 * {1}{U}{U}
 * Creature — Wall
 * 0/4
 * Defender
 * Tap an untapped Merfolk you control: This creature can attack this turn as though it didn't have
 * defender.
 * Tap an untapped Merfolk you control: This creature gets +2/+1 until end of turn.
 * When this creature dies, destroy all Merfolk tapped this turn to pay for its abilities.
 *
 * The death clause is the interesting one: it has to remember *which* Merfolk paid, across any
 * number of activations of either ability. Rather than accumulate a per-turn list on the War
 * Machine, each activation schedules its own one-shot delayed trigger naming the Merfolk that
 * paid for it — [EffectTarget.TappedAsCost] is baked into a concrete id the moment the trigger is
 * created — and every such trigger fires when the War Machine dies. Same set destroyed, no new
 * per-permanent bookkeeping. The triggers expire at end of turn, matching "tapped this turn".
 */
val VodalianWarMachine = card("Vodalian War Machine") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wall"
    oracleText = "Defender (This creature can't attack.)\n" +
        "Tap an untapped Merfolk you control: This creature can attack this turn as though it didn't have defender.\n" +
        "Tap an untapped Merfolk you control: This creature gets +2/+1 until end of turn.\n" +
        "When this creature dies, destroy all Merfolk tapped this turn to pay for its abilities."
    power = 0
    toughness = 4

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.TapPermanents(
            count = 1,
            filter = GameObjectFilter.Creature.withSubtype(Subtype.MERFOLK).untapped().youControl(),
            excludeSelf = true,
        )
        effect = Effects.Composite(
            Effects.CanAttackDespiteDefenderThisTurn(EffectTarget.Self),
            deathRevengeOn(),
        )
        description = "Tap an untapped Merfolk you control: This creature can attack this turn as though it didn't have defender."
    }

    activatedAbility {
        cost = Costs.TapPermanents(
            count = 1,
            filter = GameObjectFilter.Creature.withSubtype(Subtype.MERFOLK).untapped().youControl(),
            excludeSelf = true,
        )
        effect = Effects.Composite(
            Effects.ModifyStats(2, 1, EffectTarget.Self),
            deathRevengeOn(),
        )
        description = "Tap an untapped Merfolk you control: This creature gets +2/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd962ff0-4aa6-453e-931e-bd36fc034273.jpg?1783947906"
    }
}

/**
 * "When this creature dies, destroy [the Merfolk that just paid]" — one one-shot delayed trigger
 * per activation, watching the War Machine's own death. `fireOnce` keeps a single activation from
 * destroying its Merfolk twice; the end-of-turn expiry is the "this turn" in the printed text.
 */
private fun deathRevengeOn() = CreateDelayedTriggerEffect(
    trigger = Triggers.Dies,
    watchedTarget = EffectTarget.Self,
    effect = Effects.Destroy(EffectTarget.TappedAsCost(0)),
    expiry = DelayedTriggerExpiry.EndOfTurn,
    fireOnce = true,
)
