package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Ureni of the Unwritten {4}{G}{U}{R}
 * Legendary Creature — Spirit Dragon
 * 7/7
 *
 * Flying, trample
 * Whenever Ureni enters or attacks, look at the top eight cards of your library. You may put a
 * Dragon creature card from among them onto the battlefield. Put the rest on the bottom of your
 * library in a random order.
 *
 * "Enters or attacks" is two triggered abilities sharing one effect (Sun Titan's shape). The effect
 * is the Break Out look-and-split pipeline: an optional pick of up to one Dragon creature card
 * among the eight, which goes onto the battlefield, and everything else — including any Dragon the
 * player passed on — to the bottom in a random order.
 */
val UreniOfTheUnwritten = card("Ureni of the Unwritten") {
    manaCost = "{4}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 7
    toughness = 7
    oracleText = "Flying, trample\n" +
        "Whenever Ureni enters or attacks, look at the top eight cards of your library. You may " +
        "put a Dragon creature card from among them onto the battlefield. Put the rest on the " +
        "bottom of your library in a random order."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    val lookDescription = "Whenever Ureni enters or attacks, look at the top eight cards of your " +
        "library. You may put a Dragon creature card from among them onto the battlefield. Put " +
        "the rest on the bottom of your library in a random order."

    fun lookForDragon() = Effects.Pipeline {
        val looked = gather(CardSource.TopOfLibrary(8))
        val (dragon, rest) = chooseUpToSplit(
            count = 1,
            from = looked,
            filter = GameObjectFilter.Creature.withSubtype(Subtype.DRAGON),
            prompt = "You may put a Dragon creature card onto the battlefield",
            showAllCards = true
        )
        move(dragon, CardDestination.ToZone(Zone.BATTLEFIELD))
        toLibraryBottom(rest, order = CardOrder.Random)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = lookForDragon()
        description = lookDescription
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = lookForDragon()
        description = lookDescription
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "9"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f54ecbf1-83df-4486-bd6a-7d1f173b6863.jpg?1783907187"
    }
}
