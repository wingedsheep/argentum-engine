package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Driftgloom Coyote
 * {3}{W}{W}
 * Creature — Elemental Coyote
 * 3/4
 *
 * When this creature enters, exile target creature an opponent controls until this
 * creature leaves the battlefield. If that creature had power 2 or less, put a
 * +1/+1 counter on this creature.
 *
 * The power check uses last-known information (the creature's power as it last
 * existed on the battlefield). We check the condition before exiling so the
 * creature is still on the battlefield for the power lookup.
 */
val DriftgloomCoyote = card("Driftgloom Coyote") {
    manaCost = "{3}{W}{W}"
    typeLine = "Creature — Elemental Coyote"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, exile target creature an opponent controls until this creature leaves the battlefield. If that creature had power 2 or less, put a +1/+1 counter on this creature."

    // ETB: exile target opponent creature until this leaves + conditional +1/+1 counter
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "creature an opponent controls",
            TargetCreature(filter = TargetFilter(com.wingedsheep.sdk.scripting.GameObjectFilter.Creature.opponentControls()))
        )
        // Check power condition first (while creature is still on battlefield), then exile
        effect = ConditionalEffect(
            condition = Conditions.TargetPowerAtMost(DynamicAmount.Fixed(2)),
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        ).then(Effects.ExileUntilLeaves(creature))
    }

    // LTB: return exiled card
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Betty Jiang"
        flavorText = "\"The fog is descending like a starving beast. I believe our expedition ends here.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7ab2de3-3aea-461a-a74f-fb742cf8a198.jpg?1721425819"

        ruling("2024-07-26", "If Driftgloom Coyote leaves the battlefield before its triggered ability resolves, the target creature won't be exiled.")
        ruling("2024-07-26", "Use the power of the exiled creature as it last existed on the battlefield to determine whether or not to put a +1/+1 counter on Driftgloom Coyote.")
    }
}
