package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventEnchantment
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

val ConsecrateLand = card("Consecrate Land") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nEnchanted land has indestructible and can't be enchanted by other Auras."
    auraTarget = TargetObject(filter = TargetFilter.Land)
    staticAbility { ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.attachedCreature()) }
    staticAbility { ability = PreventEnchantment(exceptSource = true, filter = GroupFilter.attachedCreature()) }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2379f78-c03f-447f-b3c9-10a918d556e9.jpg?1783948716"
        ruling("2006-09-25", "If Consecrate Land enters attached to a land that's enchanted by other Auras, those Auras are put into their owners' graveyards.")
        ruling("2005-04-01", "The land can be targeted by land-destroying spells and the spell will resolve, but the land will simply not be destroyed.")
    }
}
