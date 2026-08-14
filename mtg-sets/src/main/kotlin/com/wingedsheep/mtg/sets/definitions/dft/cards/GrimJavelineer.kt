package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry

/**
 * Grim Javelineer
 * {2}{B}
 * Creature — Human Warrior — Common (DFT #89)
 * 3/2
 *
 * Whenever you attack, target attacking creature gets +1/+0 until end of turn. When that creature
 * dies this turn, surveil 1.
 *
 * [Triggers.YouAttack] fires once per combat in which you attacked with at least one creature, and
 * the pumped creature is chosen as a target when the ability goes on the stack (so it must still be
 * an attacking creature then). The "when that creature dies this turn" rider is a watched-entity
 * delayed triggered ability ([Triggers.Dies] scoped to the target via `watchedTarget`, expiring at
 * end of turn) — the Desperate Measures / Long River Lurker shape. Because the delayed trigger is
 * scoped by entity id, it fires on that specific game object dying; a creature that leaves and
 * returns is a new object (CR 400.7) and no longer watched.
 */
val GrimJavelineer = card("Grim Javelineer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "Whenever you attack, target attacking creature gets +1/+0 until end of turn. When " +
        "that creature dies this turn, surveil 1. (Look at the top card of your library. You may put " +
        "that card into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.YouAttack
        val attacker = target("target attacking creature", Targets.AttackingCreature)
        effect = Effects.Composite(
            listOf(
                Effects.ModifyStats(1, 0, attacker),
                CreateDelayedTriggerEffect(
                    effect = Patterns.Library.surveil(1),
                    trigger = Triggers.Dies,
                    watchedTarget = attacker,
                    expiry = DelayedTriggerExpiry.EndOfTurn
                )
            )
        )
        description = "Whenever you attack, target attacking creature gets +1/+0 until end of turn. " +
            "When that creature dies this turn, surveil 1."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Bartek Fedyczak"
        flavorText = "Kheti lost everything to the Eternals and the Phyrexians. What she gained was " +
            "an intimate knowledge of death in its myriad forms."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87154116-e306-4e15-bd5a-dcdb5ddbcd36.jpg?1783907895"
    }
}
