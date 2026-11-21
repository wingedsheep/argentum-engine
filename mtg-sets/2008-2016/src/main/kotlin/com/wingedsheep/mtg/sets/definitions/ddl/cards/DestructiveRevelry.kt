package com.wingedsheep.mtg.sets.definitions.ddl.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Destructive Revelry — Duel Decks: Heroes vs. Monsters #66 (canonical printing; reprinted in Theros)
 * {R}{G} · Instant
 *
 * Destroy target artifact or enchantment. Destructive Revelry deals 2 damage to that permanent's
 * controller.
 *
 * Same shape as Smash to Smithereens: the destroy runs first, then "that permanent's controller" is
 * read via last-known information from the battlefield-exit snapshot (the live controller when the
 * permanent survives, e.g. indestructible). An illegal target fizzles the whole spell — no damage.
 */
val DestructiveRevelry = card("Destructive Revelry") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or enchantment. Destructive Revelry deals 2 damage to that permanent's controller."

    spell {
        val permanent = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(permanent) then Effects.DealDamage(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Kev Walker"
        flavorText = "\"Stoke a fire hot enough and you'll never run out of things to burn.\"\n—Xenagos, the Reveler"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc2eb53a-3d0f-4bb3-be36-f8024f2a1d4d.jpg?1783939837"
        ruling(
            "2013-09-15",
            "You must target an artifact or enchantment to cast Destructive Revelry. If that artifact or " +
                "enchantment is an illegal target when Destructive Revelry tries to resolve, it won't resolve " +
                "and none of its effects will happen. No damage will be dealt. However, if Destructive Revelry " +
                "resolves and the artifact or enchantment isn't destroyed (perhaps because it has indestructible), " +
                "damage will be dealt."
        )
    }
}
