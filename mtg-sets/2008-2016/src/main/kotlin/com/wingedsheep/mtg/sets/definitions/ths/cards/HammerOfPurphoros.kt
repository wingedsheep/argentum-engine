package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hammer of Purphoros
 * {1}{R}{R}
 * Legendary Enchantment Artifact
 * Creatures you control have haste.
 * {2}{R}, {T}, Sacrifice a land: Create a 3/3 colorless Golem enchantment artifact creature token.
 */
val HammerOfPurphoros = card("Hammer of Purphoros") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment Artifact"
    oracleText = "Creatures you control have haste.\n" +
        "{2}{R}, {T}, Sacrifice a land: Create a 3/3 colorless Golem enchantment artifact creature token."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Land))
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Golem"),
            artifactToken = true,
            enchantmentToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/d/c/dcee70ef-6285-4f09-8a71-8b7960e8fa99.jpg?1783939698"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Yeong-Hao Han"
        flavorText = "All the world is Purphoros's anvil."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f867f61-676e-40b3-8e22-e60d0606c733.jpg?1783939762"
    }
}
