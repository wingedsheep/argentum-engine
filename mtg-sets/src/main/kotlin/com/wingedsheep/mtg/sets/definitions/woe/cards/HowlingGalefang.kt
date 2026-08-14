package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Howling Galefang
 * {2}{G}{G}
 * Creature — Beast
 * 4/4
 *
 * Vigilance
 * This creature has haste as long as you own a card in exile that has an Adventure.
 *
 * A [ConditionalStaticAbility] self-grant in the [KavuRunner][com.wingedsheep.mtg.sets.definitions.inv.cards.KavuRunner]
 * mould; the only interesting part is the condition.
 *
 * "A card in exile that has an Adventure" is [CardPredicate.HasAdventure] — a property of the *card*
 * (its layout), not of how it got there. Per the WOE ruling this deliberately does not care whether the
 * card was cast as an Adventure: a card exiled by any means at all turns haste on, which is why the
 * condition is a plain zone existence check rather than anything reading the adventure-exile marker.
 *
 * "You own" maps to `Exists(Player.You, Zone.EXILE, …)` because exile is keyed per player in this
 * engine and a card sits in its owner's exile zone, so the player scoping already means ownership
 * rather than control — the right reading here, since exiled cards have no controller.
 */
val HowlingGalefang = card("Howling Galefang") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Vigilance\n" +
        "This creature has haste as long as you own a card in exile that has an Adventure."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, Filters.Self),
            condition = Exists(
                Player.You,
                Zone.EXILE,
                GameObjectFilter(cardPredicates = listOf(CardPredicate.HasAdventure)),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"I have to hand it to you, Yorvo. Since your pet showed up, not a single " +
            "smallfolk has even made it to the door of my vault.\"\n—Beluna Grandsquall"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86311523-d0eb-4db3-b586-8349de9c2d37.jpg?1783915080"

        ruling(
            "2023-09-01",
            "Howling Galefang will have haste as long as any card you own in exile has an Adventure. " +
                "It doesn't matter if that card was cast as an Adventure or not."
        )
    }
}
