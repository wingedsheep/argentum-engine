package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akoum Hellkite — Battle for Zendikar #139
 * {4}{R}{R} · Creature — Dragon · 4/4
 * Flying
 * Landfall — Whenever a land you control enters, this creature deals 1 damage to any target. If that
 * land is a Mountain, this creature deals 2 damage instead.
 *
 * "If that land is a Mountain" reads the triggering land on resolution, the same
 * `EntityMatches(TriggeringEntity, …)` shape as Oran-Rief Hydra's Forest check and Guul Draz
 * Overseer's Swamp check; the single target is chosen when the trigger goes on the stack and only
 * one of the two damage amounts is dealt to it.
 */
val AkoumHellkite = card("Akoum Hellkite") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Landfall — Whenever a land you control enters, this creature deals 1 damage to any target. If " +
        "that land is a Mountain, this creature deals 2 damage instead."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val victim = target(Targets.Any)
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.TriggeringEntity,
                GameObjectFilter.Land.withSubtype("Mountain"),
            ),
            then = Effects.DealDamage(2, victim),
            otherwise = Effects.DealDamage(1, victim),
        )
        description = "Landfall — Whenever a land you control enters, this creature deals 1 damage to " +
            "any target. If that land is a Mountain, this creature deals 2 damage instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce837a65-a588-4b50-b298-f4ca37dbd06d.jpg?1783938196"
    }
}
