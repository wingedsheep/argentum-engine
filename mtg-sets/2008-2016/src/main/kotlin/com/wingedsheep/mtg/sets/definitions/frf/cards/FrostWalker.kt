package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frost Walker — Fate Reforged #35
 * {1}{U} · Creature — Elemental · 4 / 1
 *
 * When this creature becomes the target of a spell or ability, sacrifice it.
 *
 * The same shape as Skulking Ghost: a self "becomes the target" trigger that sacrifices itself.
 */
val FrostWalker = card("Frost Walker") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 1
    oracleText = "When this creature becomes the target of a spell or ability, sacrifice it."

    triggeredAbility {
        trigger = Triggers.self.becomesTarget()
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Mike Bierek"
        flavorText = "\"As the clans carved out their territories, we saw allies where the Mardu saw only " +
            "obstacles.\"\n—Yasova Dragonclaw"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebb2c83d-48db-4d0b-8c3a-f4b0353eeb86.jpg?1783938707"
        ruling(
            "2014-11-24",
            "If Frost Walker becomes the target of a spell or ability, its ability triggers and is put on the " +
                "stack on top of that spell or ability. Frost Walker's ability will resolve (causing it to be " +
                "sacrificed) first. Unless the spell or ability has another target, it won't resolve as it " +
                "tries to resolve for having no legal targets."
        )
    }
}
