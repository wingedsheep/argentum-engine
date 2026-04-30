package com.wingedsheep.mtg.sets.definitions.duskmourn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.SourceEnteredThisTurn

/**
 * Shardmage's Rescue
 * {W}
 * Enchantment — Aura
 * Flash
 * Enchant creature you control
 * As long as this Aura entered this turn, enchanted creature has hexproof.
 * Enchanted creature gets +1/+1.
 */
val ShardmagesRescue = card("Shardmage's Rescue") {
    manaCost = "{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature you control\nAs long as this Aura entered this turn, enchanted creature has hexproof.\nEnchanted creature gets +1/+1."

    keywords(Keyword.FLASH)
    auraTarget = Targets.CreatureYouControl

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF),
            condition = SourceEnteredThisTurn
        )
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Jarel Threat"
        flavorText = "With a flick of their wrist, Niko shattered the wickerfolk and whisked Nashi to safety."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aed0cafa-d701-4e1f-9773-abcf817c244c.jpg?1726285964"
    }
}
