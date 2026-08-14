package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tunnel Tipster — Murders at Karlov Manor #180
 * {1}{G} · Creature — Mole Scout · 1/1
 *
 * At the beginning of your end step, if a face-down creature entered the battlefield under your
 * control this turn, put a +1/+1 counter on this creature.
 * {T}: Add {G}.
 *
 * The end-step clause is an **intervening-if** (CR 603.4), so it is a `triggerCondition` rather
 * than a gate inside the effect: with no face-down creature having entered this turn the ability
 * never goes on the stack at all.
 *
 * The condition reads [Conditions.PermanentEnteredFaceDownThisTurn], the per-player face-down-entered
 * tracker. Its name says "permanent" where the card says "creature", but the two coincide by
 * construction: a face-down permanent on the battlefield is always a 2/2 colorless creature with no
 * name (CR 708.2), so any permanent that entered face down entered as a creature.
 *
 * Per the printed ruling the tracker is a *historical* fact about the turn — the face-down creature
 * having since been turned face up, or having left the battlefield, doesn't retract it; and a
 * creature that entered face **up** and was turned face down later never sets it. That is exactly
 * the entered-this-turn semantics the tracker records, which is why this can't be modelled as a
 * battlefield scan for face-down creatures.
 */
val TunnelTipster = card("Tunnel Tipster") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Mole Scout"
    oracleText = "At the beginning of your end step, if a face-down creature entered the " +
        "battlefield under your control this turn, put a +1/+1 counter on this creature.\n" +
        "{T}: Add {G}."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.PermanentEnteredFaceDownThisTurn
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "At the beginning of your end step, if a face-down creature entered the " +
            "battlefield under your control this turn, put a +1/+1 counter on this creature."
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Leesha Hannigan"
        flavorText = "\"When your perp goes underground, I know where to find 'em.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e29b890-35b9-4e2a-9b4c-9417ca7db31d.jpg?1783912858"

        ruling(
            "2024-02-02",
            "Tunnel Tipster's first ability will trigger as long as a face-down creature entered " +
                "the battlefield under your control this turn, even if that creature has turned " +
                "face up or left the battlefield since. A creature that enters the battlefield " +
                "face up and turns face down later in the turn won't cause Tunnel Tipster's first " +
                "ability to trigger."
        )
    }
}
