package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Pinecone Strike
 * {1}{R}
 * Instant
 *
 * Choose one or both —
 * • Pinecone Strike deals 3 damage to target creature. If that creature would die this turn, exile it instead.
 * • Destroy target artifact token.
 *
 * "Choose one or both" is `modal(chooseCount = 2, minChooseCount = 1)`; each chosen mode picks its own
 * target at cast time. The first mode pairs the damage with [MarkExileOnDeathEffect] (the same shape
 * Gnashing of Teeth uses) so the exile replacement applies for the rest of the turn — it is not
 * conditional on this spell's damage being lethal.
 */
val PineconeStrike = card("Pinecone Strike") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one or both —\n" +
        "• Pinecone Strike deals 3 damage to target creature. If that creature would die this turn, exile it instead.\n" +
        "• Destroy target artifact token."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode(
                "Pinecone Strike deals 3 damage to target creature. If that creature would die this turn, exile it instead"
            ) {
                val creature = target("target creature", TargetCreature(filter = TargetFilter.Creature))
                effect = Effects.Composite(
                    Effects.DealDamage(3, creature),
                    MarkExileOnDeathEffect(creature)
                )
            }
            mode("Destroy target artifact token") {
                val artifactToken = target(
                    "target artifact token",
                    TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact.token()))
                )
                effect = Effects.Destroy(artifactToken)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Javier Charro"
        flavorText = "The pinecones burst on the ground in the middle of the circle of wolves and went " +
            "off in colored sparks and smoke."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea174cea-40e5-424e-9734-e39aae6c6b17.jpg?1785496194"
    }
}
