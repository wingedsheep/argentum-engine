package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Starfall — Journey into Nyx #114 (canonical printing)
 * {4}{R} · Instant
 *
 * Starfall deals 3 damage to target creature. If that creature is an enchantment, Starfall deals
 * 3 damage to that creature's controller.
 *
 * The enchantment check runs after the damage, mid-resolution: state-based actions aren't checked
 * until Starfall finishes resolving, so a creature dealt lethal damage is still on the battlefield
 * and the check reads its projected types (an enchantment creature, or one granted the enchantment
 * type, counts). "That creature's controller" falls back to last-known information if it has left.
 */
val Starfall = card("Starfall") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Starfall deals 3 damage to target creature. If that creature is an enchantment, " +
        "Starfall deals 3 damage to that creature's controller."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, creature) then Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Enchantment, creature),
            then = Effects.DealDamage(3, EffectTarget.TargetController),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Chris Rahn"
        flavorText = "\"The most brilliant stars are not born of Nyx.\"\n—Rhexenor, Akroan flamespeaker"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13921f0d-f163-4275-b025-045c1ccd99e5.jpg?1783939417"
    }
}
