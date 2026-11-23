package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtectionFromCardType
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Holy Mantle
 * {2}{W}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2 and has protection from creatures.
 *
 * "Protection from creatures" is protection from the creature card type, the same shape as
 * Unquestioned Authority: the projected `PROTECTION_FROM_CARDTYPE_CREATURE` keyword is read by
 * blocking, damage, targeting and enchanting alike.
 */
val HolyMantle = card("Holy Mantle") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+2 and has protection from creatures."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantProtectionFromCardType(CardType.CREATURE, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Maciej Kuciara"
        flavorText = "\"The guilds promise to provide safety. I prefer to make my own.\"\n—Briska, Ninth District freemage"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95567596-c5b1-426f-bc2c-43306f7221b0.jpg?1783940143"
    }
}
