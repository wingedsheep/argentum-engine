package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Raffine's Guidance
 * {W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+1.
 * You may cast this card from your graveyard by paying {2}{W} rather than paying its mana cost.
 *
 * The graveyard clause is a keywordless alternative cost (not escape): {2}{W} replaces {W}, and
 * the Aura goes back to the graveyard if it's countered or its creature leaves, ready to be cast
 * this way again.
 */
val RaffinesGuidance = card("Raffine's Guidance") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+1.\n" +
        "You may cast this card from your graveyard by paying {2}{W} rather than paying its mana cost."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            alternativeManaCost = ManaCost.parse("{2}{W}")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Rémi Jacquot"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f36e25fa-9ab4-4832-afb9-827c4b6ba732.jpg?1783923153"
    }
}
