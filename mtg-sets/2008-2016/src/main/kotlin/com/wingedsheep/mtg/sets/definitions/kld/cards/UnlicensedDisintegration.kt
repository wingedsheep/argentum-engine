package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unlicensed Disintegration — Kaladesh #187 (canonical printing)
 * {1}{B}{R} · Instant
 *
 * Destroy target creature. If you control an artifact, Unlicensed Disintegration deals 3 damage to
 * that creature's controller.
 *
 * The destroy runs first, so the artifact check sees the board after the creature is gone (destroying
 * your only artifact creature deals no damage — 2020-08-07 ruling). "That creature's controller" is
 * read via last-known information once the creature has left the battlefield (CR 608.2h), and is the
 * live controller when it survived (indestructible).
 */
val UnlicensedDisintegration = card("Unlicensed Disintegration") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Destroy target creature. If you control an artifact, Unlicensed Disintegration deals " +
        "3 damage to that creature's controller."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Artifact),
            then = Effects.DealDamage(3, EffectTarget.TargetController),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Izzy"
        flavorText = "\"I'd stand back if I were you.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16ad8f86-7860-4896-a161-07bf347bbd5b.jpg?1783937165"
        ruling(
            "2020-08-07",
            "If the target creature is an illegal target by the time Unlicensed Disintegration tries to " +
                "resolve, the spell won't resolve. It won't deal damage to any player. If the target is legal " +
                "but not destroyed (most likely because it has indestructible), its controller is dealt 3 damage."
        )
        ruling(
            "2020-08-07",
            "Whether or not you control an artifact is checked only after the creature is destroyed while " +
                "Unlicensed Disintegration is resolving. For example, if you destroy a Fairgrounds Warden that " +
                "exiled your artifact creature, Unlicensed Disintegration deals 3 damage to Fairgrounds " +
                "Warden's controller."
        )
    }
}
