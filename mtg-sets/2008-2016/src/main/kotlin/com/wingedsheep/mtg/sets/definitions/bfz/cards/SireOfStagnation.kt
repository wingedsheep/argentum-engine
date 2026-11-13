package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sire of Stagnation
 * {4}{U}{B}
 * Creature — Eldrazi
 * 5/7
 * Devoid (This card has no color.)
 * Whenever a land an opponent controls enters, that player exiles the top two cards of their
 * library and you draw two cards.
 *
 * Nightshade Harvester's trigger (`Triggers.a(Land.opponentControls()).enters()`), with "that
 * player" read as [EffectTarget.ControllerOfTriggeringEntity] — the entering land's controller,
 * through last-known information if the land has left by resolution. The exile is the
 * Gather → Move `Patterns.Library.exileTop` pipeline keyed to that player, so a short library
 * exiles what it has and the draw still happens (ruling).
 */
val SireOfStagnation = card("Sire of Stagnation") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 7
    oracleText = "Devoid (This card has no color.)\n" +
        "Whenever a land an opponent controls enters, that player exiles the top two cards of " +
        "their library and you draw two cards."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.opponentControls()).enters()
        effect = Patterns.Library.exileTop(2, EffectTarget.ControllerOfTriggeringEntity) then
            Effects.DrawCards(2)
        description = "Whenever a land an opponent controls enters, that player exiles the top two " +
            "cards of their library and you draw two cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "206"
        artist = "Tyler Jacobson"
        flavorText = "As the great scourge passed by, Zendikar shivered, then lay silent."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6252913-7b8e-4746-91e0-658ce13cdf18.jpg?1783938181"
        ruling("2015-08-25", "The cards are exiled from the library face up.")
        ruling(
            "2015-08-25",
            "If that player has one card in their library when Sire of Stagnation's triggered ability " +
                "resolves, that card will be exiled. If their library has zero cards, no cards are " +
                "exiled. In both cases, you'll still draw two cards. That player won't lose the game " +
                "(until they attempt to draw a card from an empty library)."
        )
    }
}
