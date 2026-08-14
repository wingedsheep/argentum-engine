package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Curious Inquiry
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has "Whenever this creature deals combat damage to a
 *   player, investigate."
 *
 * The trigger is *granted to the enchanted creature* (GrantTriggeredAbility over the
 * attached-creature filter, SELF binding) rather than modelled as an Aura-bound trigger, so
 * the Clue goes to the enchanted creature's controller — which matters when this enchants a
 * creature you don't control.
 */
val CuriousInquiry = card("Curious Inquiry") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has \"Whenever this creature deals combat damage to a " +
        "player, investigate.\" (Create a Clue token. It's an artifact with \"{2}, Sacrifice this " +
        "token: Draw a card.\")"

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.DealsCombatDamageToPlayer.event,
                binding = Triggers.DealsCombatDamageToPlayer.binding,
                effect = Effects.Investigate()
            ),
            filter = Filters.EnchantedCreature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Ekaterina Burmak"
        flavorText = "Each time he replayed the attack in his mind, new details emerged."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a3603c6-92df-45c7-b402-1f0a552ea398.jpg?1783912912"
    }
}
