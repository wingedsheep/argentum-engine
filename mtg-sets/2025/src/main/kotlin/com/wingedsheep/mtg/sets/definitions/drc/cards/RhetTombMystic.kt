package com.wingedsheep.mtg.sets.definitions.drc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCyclingToCardsInHand

/**
 * Rhet-Tomb Mystic — Aetherdrift Commander #10 (canonical printing)
 * {1}{U}
 * Creature — Zombie Bird Wizard
 * 2/1
 *
 * Flying
 * Each creature card in your hand has cycling {1}{U}.
 */
val RhetTombMystic = card("Rhet-Tomb Mystic") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Bird Wizard"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Each creature card in your hand has cycling {1}{U}. ({1}{U}, Discard that card: Draw a card.)"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantCyclingToCardsInHand(
            filter = GameObjectFilter.Creature,
            cost = ManaCost.parse("{1}{U}")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Irina Nordsol"
        flavorText = "\"Kefnet may be gone, but the thirst for wisdom never dies.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/131a68e6-09ee-4afd-a0e1-d63bbc0775e8.jpg?1783907752"
    }
}
