package com.wingedsheep.mtg.sets.definitions.apc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Pernicious Deed
 * {1}{B}{G}
 * Enchantment
 * {X}, Sacrifice this enchantment: Destroy each artifact, creature, and enchantment with mana value
 * X or less.
 */
val PerniciousDeed = card("Pernicious Deed") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment"
    oracleText = "{X}, Sacrifice this enchantment: Destroy each artifact, creature, and enchantment " +
        "with mana value X or less."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.SacrificeSelf)
        effect = Effects.DestroyAll(GameObjectFilter.ArtifactCreatureOrEnchantment.manaValueAtMostX())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Christopher Moeller"
        flavorText = "\"Yawgmoth,\" Freyalise whispered as she set the bomb, \"now you will pay for your treachery.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae4cbb33-4947-49f0-b612-a92141fbfaa6.jpg?1783945332"
        ruling("2018-03-16", "If a permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2018-03-16", "A token has a mana value of 0 unless it is copying something else.")
    }
}
