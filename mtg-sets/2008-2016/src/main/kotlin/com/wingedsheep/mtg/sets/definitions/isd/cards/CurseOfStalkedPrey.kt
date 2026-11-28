package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curse of Stalked Prey
 * {1}{R}
 * Enchantment — Aura Curse
 * Enchant player
 * Whenever a creature deals combat damage to enchanted player, put a +1/+1 counter on that creature.
 *
 * Curse of Hospitality's any-creature combat-damage observer narrowed to [Recipient.EnchantedPlayer],
 * paying off like Rakish Heir: the damaging creature is the triggering entity, whoever controls it.
 */
val CurseOfStalkedPrey = card("Curse of Stalked Prey") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nWhenever a creature deals combat damage to enchanted player, put a +1/+1 " +
        "counter on that creature."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dealsCombatDamage(Recipient.EnchantedPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        description = "Whenever a creature deals combat damage to enchanted player, put a +1/+1 counter on " +
            "that creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Christopher Moeller"
        flavorText = "\"Innocent thorns can fill the air with your bloodscent. Don't stray from the path.\"\n" +
            "—Elmut, crossway watcher"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11a18883-8990-40a0-bcb2-e01d0e82bfad.jpg"
    }
}
