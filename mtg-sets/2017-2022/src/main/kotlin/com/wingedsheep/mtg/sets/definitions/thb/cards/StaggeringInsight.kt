package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Staggering Insight
 * {W}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has lifelink and "Whenever this creature deals combat damage to
 * a player, draw a card."
 *
 * Mark of the Vampire's +N/+N-and-lifelink statics plus Thief's Knife's granted combat-damage draw
 * trigger ([GrantTriggeredAbility] over the enchanted creature). Each Insight grants its own copy of
 * the trigger, so two Insights draw two cards (ruling).
 */
val StaggeringInsight = card("Staggering Insight") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has lifelink and \"Whenever this creature deals combat damage " +
        "to a player, draw a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Dmitry Burmak"
        flavorText = "\"Only cowards flee from the truth.\"\n—Perisophia the philosopher"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/125b8be1-76b9-434e-a505-5099d950767c.jpg?1783931517"
        ruling(
            "2020-01-24",
            "Multiple instances of lifelink on the same creature are redundant. On the other hand, multiple " +
                "instances of the triggered ability trigger separately if a creature is enchanted by more than " +
                "one Staggering Insight."
        )
    }
}
