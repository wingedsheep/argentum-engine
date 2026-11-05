package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dune Diviner — Hour of Devastation #114 (canonical printing)
 * {2}{G} · Creature — Snake Cleric · 2/3
 *
 * {1}, Tap an untapped Desert you control: You gain 1 life.
 */
val DuneDiviner = card("Dune Diviner") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Cleric"
    power = 2
    toughness = 3
    oracleText = "{1}, Tap an untapped Desert you control: You gain 1 life."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.TapPermanents(1, GameObjectFilter.Land.withSubtype("Desert")),
        )
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Min Yum"
        flavorText = "The desert had once been a place of endless horror. It was now the last refuge as the city of Naktamun fell."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01818188-e18e-4dc0-b7ef-34cedef64f7e.jpg?1783936021"
        ruling("2017-07-14", "You can’t tap a single untapped Desert both to pay {1} and also to pay “Tap an untapped Desert you control.”")
    }
}
