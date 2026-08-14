package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Disarm — Mirrodin #32 (canonical printing, only printing)
 * {U} · Instant
 *
 * Unattach all Equipment from target creature.
 *
 * Composed rather than given its own effect type: [CardSource.AttachedTo] gathers the Equipment
 * currently attached to the target (the same gather Light of Judgment uses), then
 * [ForEachInCollectionEffect] runs [Effects.UnattachEquipment] once per gathered Equipment with
 * `EffectTarget.Self` bound to the iteration entity. No selection step — the oracle text says
 * "all", so nothing is chosen and nothing is targeted beyond the creature itself.
 *
 * Per the 2004-12-01 ruling, each Equipment stays on the battlefield under its controller's
 * control; only the attachment link is broken (CR 701.3d) — which is exactly what
 * `UnattachEquipment` does. Auras on the creature are untouched: the gather filters to the
 * Equipment subtype. Resolving with no Equipment attached (or with the creature's Equipment
 * already gone) is a silent no-op; the spell still fizzles outright if the creature itself is an
 * illegal target on resolution.
 */
val Disarm = card("Disarm") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Unattach all Equipment from target creature."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Pipeline {
            val equipment = gather(
                CardSource.AttachedTo(
                    host = creature,
                    filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                )
            )
            run(
                ForEachInCollectionEffect(
                    collection = equipment.key,
                    effect = Effects.UnattachEquipment(EffectTarget.Self),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Alex Horley-Orlandelli"
        flavorText = "\"Be thankful I left you your clothes.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/2/427c6350-af52-45f1-8024-5f31aa62a0d0.jpg?1783944556"
        ruling(
            "2004-12-01",
            "The Equipment remains on the battlefield under its controller's control, but is no " +
                "longer attached to that creature."
        )
    }
}
