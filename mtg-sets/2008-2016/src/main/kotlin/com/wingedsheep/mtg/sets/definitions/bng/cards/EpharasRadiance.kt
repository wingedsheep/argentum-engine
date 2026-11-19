package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ephara's Radiance
 * {W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "{1}{W}, {T}: You gain 3 life."
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val EpharasRadiance = card("Ephara's Radiance") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"{1}{W}, {T}: You gain 3 life.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
                effect = Effects.GainLife(3)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "James Ryman"
        flavorText = "\"Civilization is the sum of Ephara's gifts.\"\n—Olexa of the Twelve"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0741582-0c7b-4d5b-83a9-dd8862de1f24.jpg?1783939583"
    }
}
