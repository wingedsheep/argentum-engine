package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Hellish Sideswipe
 * {B}
 * Sorcery
 *
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * Destroy target creature or Vehicle. If the sacrificed permanent was a Vehicle, draw a card.
 *
 * The additional sacrifice cost snapshots the sacrificed permanent's projected characteristics into
 * `EffectContext.sacrificedPermanents` at payment time (CR 601.2h), so the rider reads
 * last-known information via [Conditions.SacrificedHadSubtype] rather than looking for a permanent
 * that is already in the graveyard. That matters for a Vehicle that was an artifact *creature* when
 * sacrificed (crewed, or animated by its own exhaust ability) — it still had the Vehicle subtype, so
 * the draw happens; and for a creature that merely *became* a Vehicle, likewise.
 *
 * A Vehicle is an artifact, so the printed "sacrifice an artifact or creature" cost is
 * [GameObjectFilter.CreatureOrArtifact] — no separate Vehicle branch is needed to make the rider
 * reachable.
 */
val HellishSideswipe = card("Hellish Sideswipe") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\n" +
        "Destroy target creature or Vehicle. If the sacrificed permanent was a Vehicle, draw a card."

    additionalCost(
        Costs.additional.SacrificePermanent(filter = GameObjectFilter.CreatureOrArtifact)
    )

    spell {
        target(
            "creature or Vehicle",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle)),
        )
        effect = Effects.Destroy(EffectTarget.ContextTarget(0)).then(
            ConditionalEffect(
                condition = Conditions.SacrificedHadSubtype(Subtype.VEHICLE.value),
                effect = Effects.DrawCards(1),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Diana Franco"
        flavorText = "Might makes right-of-way."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a9db650-47f9-46d7-ac17-8d19fef6d6b0.jpg?1783907894"
    }
}
