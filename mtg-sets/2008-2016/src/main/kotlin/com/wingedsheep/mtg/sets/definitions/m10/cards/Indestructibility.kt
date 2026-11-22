package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Indestructibility
 * {3}{W}
 * Enchantment — Aura
 * Enchant permanent
 * Enchanted permanent has indestructible.
 */
val Indestructibility = card("Indestructibility") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant permanent\nEnchanted permanent has indestructible. (Effects that say \"destroy\" don't destroy that permanent. A creature with indestructible can't be destroyed by damage.)"
    auraTarget = TargetObject(filter = TargetFilter.Permanent)
    staticAbility { ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.attachedCreature()) }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef8765a0-c2ae-4b1a-a3f5-0243b43e6da0.jpg?1783942401"
        ruling("2013-07-01", "If a creature enchanted by Indestructibility is dealt lethal damage, the creature isn't destroyed, but the damage remains marked on the creature. If Indestructibility stops enchanting that creature later in the turn, the creature will no longer have indestructible and will be destroyed.")
        ruling("2009-10-01", "If an effect would simultaneously destroy Indestructibility and the creature it's enchanting, only Indestructibility is destroyed.")
    }
}
