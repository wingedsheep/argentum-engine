package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Homarid Spawning Bed
 * {U}{U}
 * Enchantment
 * {1}{U}{U}, Sacrifice a blue creature: Create X 1/1 blue Camarid creature tokens, where X is the
 * sacrificed creature's mana value.
 *
 * Priest of Yawgmoth's shape: the sacrificed permanent's last-known mana value is captured at cost
 * payment and read back by [EntityReference.Sacrificed], so a token (mana value 0) makes nothing.
 *
 * The 1/1 blue Camarid has never been printed as a token card, so there is no art for it anywhere
 * on Scryfall; it resolves through the engine-wide `TokenArt` table, which points it at Fallen
 * Empires' own Homarid — the crustacean that breeds them.
 */
val HomaridSpawningBed = card("Homarid Spawning Bed") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "{1}{U}{U}, Sacrifice a blue creature: Create X 1/1 blue Camarid creature tokens, " +
        "where X is the sacrificed creature's mana value."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{U}{U}"),
            Costs.Sacrifice(GameObjectFilter.Creature.withColor(Color.BLUE))
        )
        effect = Effects.CreateToken(
            count = DynamicAmount.EntityProperty(
                EntityReference.Sacrificed(0),
                EntityNumericProperty.ManaValue
            ),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Camarid")
        )
        description = "{1}{U}{U}, Sacrifice a blue creature: Create X 1/1 blue Camarid creature tokens, where X is the sacrificed creature's mana value."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cbb62fc-3cd9-41a6-804a-4ff9a766897f.jpg?1783947911"
    }
}
