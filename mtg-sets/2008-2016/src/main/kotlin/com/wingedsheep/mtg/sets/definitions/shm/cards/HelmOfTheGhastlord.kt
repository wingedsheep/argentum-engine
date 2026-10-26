package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Helm of the Ghastlord (SHM #166)
 * {3}{U/B}
 * Enchantment — Aura
 * Enchant creature
 * As long as enchanted creature is blue, it gets +1/+1 and has "Whenever this creature deals damage
 * to an opponent, draw a card."
 * As long as enchanted creature is black, it gets +1/+1 and has "Whenever this creature deals damage
 * to an opponent, that player discards a card."
 *
 * Each colour clause is a pair of conditional statics on [Conditions.EnchantedPermanentMatches]
 * (read in projected state), so a blue-black creature gets both bonuses. The granted abilities are
 * the creature's own, so "an opponent" is an opponent of the creature's controller and any damage —
 * not just combat damage — triggers them. The grant is only present while the colour holds, so the
 * creature's colour at the moment damage is dealt is what decides whether each one triggers.
 */
val HelmOfTheGhastlord = card("Helm of the Ghastlord") {
    manaCost = "{3}{U/B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "As long as enchanted creature is blue, it gets +1/+1 and has \"Whenever this creature deals " +
        "damage to an opponent, draw a card.\"\n" +
        "As long as enchanted creature is black, it gets +1/+1 and has \"Whenever this creature deals " +
        "damage to an opponent, that player discards a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    val isBlue = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature.withColor(Color.BLUE))
    val isBlack = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature.withColor(Color.BLACK))

    staticAbility {
        ability = ModifyStats(1, 1)
        condition = isBlue
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            TriggeredAbility.create(
                trigger = Triggers.self.dealsDamage(Recipient.Opponent),
                effect = Effects.DrawCards(1)
            )
        )
        condition = isBlue
    }

    staticAbility {
        ability = ModifyStats(1, 1)
        condition = isBlack
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            TriggeredAbility.create(
                trigger = Triggers.self.dealsDamage(Recipient.Opponent),
                effect = Patterns.Hand.discardCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
            )
        )
        condition = isBlack
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/653dd5b7-1ad3-4df1-bd2a-4e6ae362a8a2.jpg?1783942732"

        ruling("2008-05-01", "If the enchanted creature is both of the listed colors, it will get both bonuses.")
        ruling("2008-05-01", "With regards to the two triggered abilities that Helm of the Ghastlord grants to the enchanted creature, the point when damage is actually dealt is the only time it matters what color the creature is. If the creature is blue or black at that time, the appropriate ability or abilities will trigger. They'll resolve even if the creature changes color or loses its Aura.")
        ruling("2008-05-01", "The two abilities trigger when the enchanted creature deals damage to an opponent of the creature's controller, not when it deals damage to an opponent of the Helm's controller. They trigger from any damage, not just combat damage.")
    }
}
