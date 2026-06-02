package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Gift of Strands
 * {3}{G}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * When this Aura enters, scry 2.
 * Enchanted creature gets +3/+3.
 */
val GiftOfStrands = card("Gift of Strands") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\nWhen this Aura enters, scry 2.\nEnchanted creature gets +3/+3."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.scry(2)
    }

    staticAbility {
        ability = ModifyStats(3, 3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Ignis Bruno"
        flavorText = "\"None have ever made to me a request so bold and yet so courteous.\"\n—Galadriel"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c54ee0c-1432-4f20-9a92-2cdfcbab30ac.jpg?1686969408"
    }
}
