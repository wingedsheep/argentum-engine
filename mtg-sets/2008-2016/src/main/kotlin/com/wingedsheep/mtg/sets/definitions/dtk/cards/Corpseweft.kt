package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Corpseweft — Dragons of Tarkir #92
 * {2}{B} · Enchantment · Rare
 *
 * {1}{B}, Exile one or more creature cards from your graveyard: Create a tapped X/X black Zombie
 * Horror creature token, where X is twice the number of cards exiled this way.
 *
 * "Exile one or more" is [Costs.ExileXFromGraveyard] under `minimumXValue = 1`: there is no `{X}`
 * mana, so activating pauses for the creature cards, the number picked *is* the ability's X (at
 * least one, so the ability can't be activated from a graveyard with no creature card), and the
 * token's size is twice that X, fixed as it's created. "Exiled this way" is that activation's
 * selection only, never cards an earlier activation exiled (DTK ruling).
 */
val Corpseweft = card("Corpseweft") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "{1}{B}, Exile one or more creature cards from your graveyard: Create a tapped X/X " +
        "black Zombie Horror creature token, where X is twice the number of cards exiled this way."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.ExileXFromGraveyard(GameObjectFilter.Creature),
        )
        minimumXValue = 1
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie", "Horror"),
            tapped = true,
            dynamicPower = DynamicAmounts.xValue() * 2,
            dynamicToughness = DynamicAmounts.xValue() * 2,
            imageUri = "https://cards.scryfall.io/normal/front/5/3/533c41ab-d9ee-4817-a982-41d4da6fa9ad.jpg?1783938561",
        )
        description = "{1}{B}, Exile one or more creature cards from your graveyard: Create a tapped X/X " +
            "black Zombie Horror creature token, where X is twice the number of cards exiled this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25cf24e7-2e6e-46e0-aedd-89e17953811c.jpg?1783938599"
        ruling("2015-02-25", "Because exiling the creature cards from your graveyard is part of the activation cost, no player can respond to stop you from activating the ability by moving creature cards out of your graveyard.")
        ruling("2015-02-25", "“Exiled this way” refers to that activation of the ability, not any creature cards that have been previously exiled.")
    }
}
