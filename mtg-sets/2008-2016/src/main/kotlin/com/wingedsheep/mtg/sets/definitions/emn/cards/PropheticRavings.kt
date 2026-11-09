package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Prophetic Ravings
 * {R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has haste and "{T}, Discard a card: Draw a card."
 */
val PropheticRavings = card("Prophetic Ravings") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has haste and \"{T}, Discard a card: Draw a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Composite(Costs.Tap, Costs.DiscardCard),
                effect = Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Lucas Graciano"
        flavorText = "\"Keep your herons and shrews! You will find nothing in the moon, for the moon is a lie! It has told me so!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9e58861-a548-4344-a37b-1c5764b144d9.jpg?1783937459"
    }
}
