package com.wingedsheep.mtg.sets.definitions.snc.cards

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
 * Sticky Fingers
 * {R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has menace and "Whenever this creature deals combat damage to a player,
 *   create a Treasure token."
 * When enchanted creature dies, draw a card.
 *
 * The Treasure trigger is granted to the enchanted creature, so the Treasure goes to that
 * creature's controller; the dies trigger is the Aura's own, so the Aura's controller draws.
 */
val StickyFingers = card("Sticky Fingers") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has menace and \"Whenever this creature deals combat damage to a player, " +
        "create a Treasure token.\" (It can't be blocked except by two or more creatures. The token is " +
        "an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "When enchanted creature dies, draw a card."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.CreateTreasure()
            ),
            filter = Filters.EnchantedCreature
        )
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3678fa3d-d41f-4b7a-b25e-6fc5f78876c7.jpg?1783923112"
    }
}
