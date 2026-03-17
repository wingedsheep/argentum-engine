package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Danitha Capashen, Paragon
 * {2}{W}
 * Legendary Creature — Human Knight
 * 2/2
 * First strike, vigilance, lifelink
 * Aura and Equipment spells you cast cost {1} less to cast.
 */
val DanithaCapashenParagon = card("Danitha Capashen, Paragon") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "First strike, vigilance, lifelink\nAura and Equipment spells you cast cost {1} less to cast."

    keywords(Keyword.FIRST_STRIKE, Keyword.VIGILANCE, Keyword.LIFELINK)

    staticAbility {
        ability = ReduceSpellCostByFilter(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(listOf(
                        CardPredicate.HasSubtype(Subtype.AURA),
                        CardPredicate.HasSubtype(Subtype.EQUIPMENT)
                    ))
                )
            ),
            amount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Chris Rallis"
        flavorText = "\"I will protect the less fortunate. I will love bravely. I will face despair and fight on. As a Capashen, I can do no less.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2a2b53c-0bf2-4d3d-91c2-57a484ae4f6b.jpg?1562741472"
    }
}
