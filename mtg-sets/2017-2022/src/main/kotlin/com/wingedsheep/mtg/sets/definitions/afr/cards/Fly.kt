package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fly (AFR #59) — {U} Enchantment — Aura (uncommon)
 *
 * Enchant creature
 * Enchanted creature has flying and "Whenever this creature deals combat damage to a player,
 * venture into the dungeon."
 *
 * The venture trigger is granted to the enchanted creature (the Sticky Fingers idiom), so the
 * creature's controller ventures — not necessarily the Aura's controller.
 */
val Fly = card("Fly") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has flying and \"Whenever this creature deals combat damage to a player, " +
        "venture into the dungeon.\" (Enter the first room or advance to the next room.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.VentureIntoTheDungeon()
            ),
            filter = Filters.EnchantedCreature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1a91cc6-67e1-4f1b-86e2-eb388a75d6ad.jpg?1783926514"
    }
}
