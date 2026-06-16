package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect


/**
 * Ahriman
 * {2}{B}
 * Creature — Eye Horror
 * 2/2
 * Flying, deathtouch
 * {3}, Sacrifice another creature or artifact: Draw a card.
 */
val Ahriman = card("Ahriman") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eye Horror"
    oracleText = "Flying, deathtouch\n{3}, Sacrifice another creature or artifact: Draw a card."
    power = 2
    toughness = 2
    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact))
        effect = DrawCardsEffect(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Kevin Sidharta"
        flavorText = "\"The Wrath of Dark has nearly reached its zenith! No one can stop it! Least of all you!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/162a415c-5465-497e-8f4e-c6f09681641d.jpg"
    }
}
