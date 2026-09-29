package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crystal Carapace
 * {3}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +3/+3 and has ward {2}.
 * Cycling {2}
 */
val CrystalCarapace = card("Crystal Carapace") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +3/+3 and has ward {2}.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 3)
    }

    // GrantWard's default scope is the attached creature.
    staticAbility {
        ability = GrantWard(WardCost.Mana("{2}"))
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Sam Burley"
        flavorText = "\"Why won't you die, wretched flesh-beast?\"\n—Lukka"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1888c61-dc2d-4a33-9ba4-439d97f490be.jpg?1783916972"
    }
}
