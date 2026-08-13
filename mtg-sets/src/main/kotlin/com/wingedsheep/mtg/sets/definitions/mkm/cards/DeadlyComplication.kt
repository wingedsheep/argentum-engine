package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Deadly Complication — Murders at Karlov Manor #195
 * {1}{B}{R} · Sorcery
 *
 * Choose one or both —
 * • Destroy target creature.
 * • Put a +1/+1 counter on target suspected creature you control. You may have it become no longer
 *   suspected.
 *
 * The second mode is the set's suspect cycle paying itself off: the creature you suspected earlier
 * for a cheap rate gets a counter *and* the chance to shed the drawback, converting a temporary
 * liability into a permanent body.
 *
 * `modal(chooseCount = 2, minChooseCount = 1)` is the standard "choose one or both" shape — at least
 * one mode, up to both, each with its own target chosen at cast time. Because the modes are targeted
 * separately, the destroy mode may kill the very creature the other mode is pumping; modes resolve in
 * printed order, so the counter lands first and the destruction still happens.
 *
 * Mode 2's target is narrowed to `Creature.youControl().suspected()` — not any creature. A creature
 * that stops being suspected between cast and resolution becomes an illegal target and that mode
 * simply does nothing (the other mode, if chosen, still resolves).
 *
 * "You may have it become no longer suspected" is a genuine choice at resolution, hence [MayEffect]
 * wrapping [Effects.NoLongerSuspected] rather than an automatic strip — keeping the suspect is
 * sometimes correct, since menace on an evasive attacker can be worth more than the ability to
 * block. [Effects.NoLongerSuspected] (CR 701.60c) removes status, menace, and "can't block" together.
 */
val DeadlyComplication = card("Deadly Complication") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Destroy target creature.\n" +
        "• Put a +1/+1 counter on target suspected creature you control. You may have it become no " +
        "longer suspected."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Destroy target creature") {
                val victim = target("target creature", TargetCreature())
                effect = Effects.Destroy(victim)
            }
            mode("Put a +1/+1 counter on target suspected creature you control") {
                val suspect = target(
                    "target suspected creature you control",
                    TargetCreature(
                        filter = TargetFilter(GameObjectFilter.Creature.youControl().suspected())
                    )
                )
                effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, suspect)
                    .then(
                        MayEffect(
                            Effects.NoLongerSuspected(suspect),
                            descriptionOverride = "Have it become no longer suspected?"
                        )
                    )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Jodie Muir"
        flavorText = "Teysa's security was unrivaled. Etrata was locked up. Kaya was left with " +
            "nothing but multiplying questions... and a dead friend."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c68981c-037c-42e7-9b7f-6f07edab5f2e.jpg?1783912852"

        ruling(
            "2024-02-02",
            "You choose modes as you cast Deadly Complication. You must choose at least one mode, " +
                "and you may choose both. You choose targets for each mode you chose."
        )
        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until it " +
                "leaves the battlefield or another effect causes it to no longer be suspected."
        )
    }
}
