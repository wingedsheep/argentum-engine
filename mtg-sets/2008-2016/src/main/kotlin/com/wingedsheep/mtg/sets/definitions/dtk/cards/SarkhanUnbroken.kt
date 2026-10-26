package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sarkhan Unbroken {2}{G}{U}{R}
 * Legendary Planeswalker — Sarkhan
 * Starting Loyalty: 4
 *
 * +1: Draw a card, then add one mana of any color.
 * −2: Create a 4/4 red Dragon creature token with flying.
 * −8: Search your library for any number of Dragon creature cards, put them onto the battlefield,
 *     then shuffle.
 *
 * The −8 search is unbounded, so it is Nissa, Who Shakes the World's inline gather → chooseAnyNumber
 * → move pipeline rather than `Patterns.Library.searchLibrary` (which only offers "up to N").
 */
val SarkhanUnbroken = card("Sarkhan Unbroken") {
    manaCost = "{2}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Planeswalker — Sarkhan"
    startingLoyalty = 4
    oracleText = "+1: Draw a card, then add one mana of any color.\n" +
        "−2: Create a 4/4 red Dragon creature token with flying.\n" +
        "−8: Search your library for any number of Dragon creature cards, put them onto the " +
        "battlefield, then shuffle."

    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1) then Effects.AddAnyColorMana(1)
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1ee0725f-8562-49c2-83fa-11a702e5c78d.jpg?1783938561"
        )
    }

    loyaltyAbility(-8) {
        effect = Effects.Pipeline {
            val dragons = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Creature.withSubtype(Subtype.DRAGON)
                ),
                search = true
            )
            val found = chooseAnyNumber(
                from = dragons,
                prompt = "Search your library for any number of Dragon creature cards",
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, player = Player.You))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "230"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/192452f8-93c2-4a20-a52b-0093741a9e45.jpg?1783938570"
    }
}
