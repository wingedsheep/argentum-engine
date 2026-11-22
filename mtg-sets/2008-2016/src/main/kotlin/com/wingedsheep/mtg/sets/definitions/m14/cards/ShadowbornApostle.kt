package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Shadowborn Apostle
 * {B}
 * Creature — Human Cleric
 * 1/1
 * A deck can have any number of cards named Shadowborn Apostle.
 * {B}, Sacrifice six creatures named Shadowborn Apostle: Search your library for a Demon
 * creature card, put it onto the battlefield, then shuffle.
 *
 * The deck-construction clause is read from the oracle text by the server's DeckValidator,
 * like Rat Colony and Persistent Petitioners. The sacrifice cost may include the activating
 * Apostle itself, or be six other Apostles.
 */
val ShadowbornApostle = card("Shadowborn Apostle") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "A deck can have any number of cards named Shadowborn Apostle.\n" +
        "{B}, Sacrifice six creatures named Shadowborn Apostle: Search your library for a Demon creature card, put it onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.SacrificeMultiple(6, GameObjectFilter.Creature.named("Shadowborn Apostle")),
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.DEMON),
            destination = SearchDestination.BATTLEFIELD,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/202c2323-6589-457a-af51-5528a98e7b30.jpg?1783939920"
        ruling("2013-07-01", "Shadowborn Apostle's first ability lets you ignore only the \"four-of\" rule. It doesn't let you ignore format legality.")
        ruling("2013-07-01", "You sacrifice the six creatures when you activate the ability. You can't respond to that ability by activating the ability of one of the other Shadowborn Apostles you just sacrificed.")
        ruling("2013-07-01", "You don't have to sacrifice the Shadowborn Apostle whose ability you activated. It can be six other Shadowborn Apostles.")
    }
}
