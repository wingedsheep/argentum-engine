package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sentinel's Eyes
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has vigilance.
 * Escape—{W}, Exile two other cards from your graveyard.
 *
 * A plain +1/+1-and-vigilance Aura plus Satyr's Cunning's escape keyword. Escaped from the
 * graveyard, it's still an Aura spell that targets as it's cast; it enters attached and goes back
 * to the graveyard (not exile) when it later leaves, ready to escape again (ruling).
 */
val SentinelsEyes = card("Sentinel's Eyes") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has vigilance.\n" +
        "Escape—{W}, Exile two other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EnchantedCreature)
    }

    keywordAbility(KeywordAbility.escape("{W}", Costs.additional.ExileOtherCards(2)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32adc118-b81e-48c2-b7ef-b62e8c3308d6.jpg?1783931590"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
