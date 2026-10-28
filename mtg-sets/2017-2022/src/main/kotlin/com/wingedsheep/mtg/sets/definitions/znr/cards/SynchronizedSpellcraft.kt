package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Synchronized Spellcraft — Zendikar Rising #168
 * {4}{R} · Instant
 *
 * Synchronized Spellcraft deals 4 damage to target creature and X damage to that creature's
 * controller, where X is the number of creatures in your party.
 *
 * "That creature's controller" is [EffectTarget.TargetController] (Agonizing Demise's shape); X is
 * [DynamicAmounts.partySize] read on resolution. An illegal target fizzles the whole spell, so no
 * player is dealt damage.
 */
val SynchronizedSpellcraft = card("Synchronized Spellcraft") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Synchronized Spellcraft deals 4 damage to target creature and X damage to that creature's " +
        "controller, where X is the number of creatures in your party. (Your party consists of up to one each " +
        "of Cleric, Rogue, Warrior, and Wizard.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature) then
            Effects.DealDamage(DynamicAmounts.partySize(), EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Svetlin Velinov"
        flavorText = "\"Ready? One... two...\""
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d87f4637-1ad3-4d61-98ff-98c0b0be46a1.jpg?1783929348"
        ruling(
            "2020-09-25",
            "If the target creature is an illegal target by the time Synchronized Spellcraft tries to resolve, " +
                "the spell doesn't resolve. No player is dealt damage."
        )
    }
}
