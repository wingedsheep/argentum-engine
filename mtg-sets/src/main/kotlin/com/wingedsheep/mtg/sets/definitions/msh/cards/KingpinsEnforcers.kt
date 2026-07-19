package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kingpin's Enforcers
 * {2}{B}
 * Creature — Human Villain
 * 2/3
 * Lifelink
 * {2}{B}, Sacrifice an artifact or creature: Draw a card.
 *
 * The sacrifice cost is unrestricted beyond "artifact or creature you control" — this creature
 * itself is a legal sacrifice.
 */
val KingpinsEnforcers = card("Kingpin's Enforcers") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Villain"
    oracleText = "Lifelink\n{2}{B}, Sacrifice an artifact or creature: Draw a card."
    power = 2
    toughness = 3

    keywords(Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.Sacrifice(GameObjectFilter.CreatureOrArtifact)
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Kevin Sidharta"
        flavorText = "\"Mister Fisk heard that you have an issue with the way he does business. We've come to resolve this dispute.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64056edb-6de2-4694-b771-f35542c25771.jpg?1783902942"
    }
}
