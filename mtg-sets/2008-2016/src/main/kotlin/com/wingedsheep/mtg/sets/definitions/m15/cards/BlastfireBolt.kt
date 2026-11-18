package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blastfire Bolt — Magic 2015 #130 (canonical printing)
 * {5}{R} · Instant
 *
 * Blastfire Bolt deals 5 damage to target creature. Destroy all Equipment attached to that creature.
 *
 * Light of Judgment's shape with "all" in place of "up to one": deal 5 to the target, then gather the
 * Equipment attached to that same creature ([CardSource.AttachedTo]) and destroy the whole collection
 * at once. State-based actions don't run mid-resolution, so a creature the damage made lethal still has
 * its Equipment attached when the gather reads it. The destroy is not targeted — no choice, no
 * targeting restrictions. A creature with no Equipment just takes the damage; an illegal target on
 * resolution fizzles the whole spell, so no Equipment is destroyed.
 */
val BlastfireBolt = card("Blastfire Bolt") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Blastfire Bolt deals 5 damage to target creature. Destroy all Equipment attached to that creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, creature) then
            Effects.Pipeline {
                val equipment = gather(
                    CardSource.AttachedTo(
                        host = creature,
                        filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                    )
                )
                destroy(equipment)
            }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Phill Simmer"
        flavorText = "\"Encase yourself in the most elaborate armor, and cower behind the heaviest shield. " +
            "I would hate for you to feel helpless.\"\n—Korig the Ruiner"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34774f01-5976-4ed1-b0f4-71dc7a679999.jpg?1783939177"
        ruling(
            "2014-07-18",
            "Blastfire Bolt can target any creature, not just one with Equipment attached to it. The damage " +
                "will be dealt to the creature even if it doesn't have any Equipment attached to it."
        )
        ruling(
            "2014-07-18",
            "If the target creature is an illegal target when Blastfire Bolt tries to resolve, it won't " +
                "resolve and none of its effects will happen. No Equipment will be destroyed."
        )
    }
}
