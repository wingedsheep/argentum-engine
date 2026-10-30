package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dire Tactics
 * {W}{B}
 * Instant
 * Exile target creature. If you don't control a Human, you lose life equal to that creature's
 * toughness.
 *
 * Printed order is kept: the Human check happens after the exile (ruling 2020-04-17), and the
 * toughness read on the target after this resolution moved it off the battlefield is its
 * last-known value (a context target's own-move LKI, CR 608.2h), so pumps and Auras count.
 */
val DireTactics = card("Dire Tactics") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Exile target creature. If you don't control a Human, you lose life equal to " +
        "that creature's toughness."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Exile(creature) then Effects.If(
            condition = Conditions.Not(
                Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Human"))
            ),
            then = Effects.LoseLife(DynamicAmounts.toughnessOf(creature), EffectTarget.Controller),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Daarken"
        flavorText = "\"I will preserve human life at all costs. I will stand alone against behemoths " +
            "before I let them touch my city. I will hold the wall.\"\n—Oath of the Coppercoats"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0e3974e-3753-4f25-8930-6d96b40332ce.jpg?1783931025"
        ruling("2020-04-17", "Use the exiled creature’s toughness as it last existed on the battlefield to determine how much life you lose.")
        ruling("2020-04-17", "Whether you control a Human is checked only after exiling the target creature. If that creature had exiled a Human you controlled until that creature left the battlefield, that Human will return to the battlefield in time to be counted.")
    }
}
