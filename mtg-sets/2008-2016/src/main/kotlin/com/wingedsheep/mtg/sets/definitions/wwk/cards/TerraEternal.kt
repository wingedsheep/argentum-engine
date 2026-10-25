package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Terra Eternal
 * {2}{W}
 * Enchantment
 * All lands have indestructible.
 *
 * A [GrantKeyword] over every land on the battlefield, whoever controls it.
 */
val TerraEternal = card("Terra Eternal") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "All lands have indestructible."

    staticAbility {
        ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.AllLands)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Daniel Ljunggren"
        flavorText = "\"If this world could make a wish, it would be to survive the parasites who loot its " +
            "treasures and threaten its life force.\"\n—Saidah, Joraga hunter"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bad0759-4132-42b5-9a26-0fcf321a60e0.jpg?1783942063"
        ruling(
            "2013-07-01",
            "Effects that say \"destroy\" won't cause a land with indestructible to be put into its owner's " +
                "graveyard. If that land is also a creature, lethal damage won't cause it to be put into its " +
                "owner's graveyard either. However, a land with indestructible can be put into the graveyard " +
                "for a number of reasons. The most likely reasons are if it's sacrificed, if it's legendary " +
                "and another legendary land with the same name is controlled by the same player, or if it's " +
                "also a creature and its toughness is 0 or less."
        )
    }
}
