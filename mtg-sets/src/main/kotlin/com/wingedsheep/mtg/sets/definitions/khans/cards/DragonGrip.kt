package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dragon Grip
 * {2}{R}
 * Enchantment — Aura
 * Ferocious — If you control a creature with power 4 or greater, you may cast
 * Dragon Grip as though it had flash.
 * Enchant creature
 * Enchanted creature gets +2/+0 and has first strike.
 */
val DragonGrip = card("Dragon Grip") {
    manaCost = "{2}{R}"
    typeLine = "Enchantment — Aura"
    oracleText = "Ferocious — If you control a creature with power 4 or greater, you may cast this spell as though it had flash.\nEnchant creature\nEnchanted creature gets +2/+0 and has first strike."

    auraTarget = Targets.Creature

    conditionalFlash = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4))

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0269d5bd-d8aa-465b-bfe9-6703937f933c.jpg?1562781842"
    }
}
