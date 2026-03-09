package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kheru Spellsnatcher
 * {3}{U}
 * Creature — Snake Wizard
 * 3/3
 * Morph {4}{U}{U}
 * When this creature is turned face up, counter target spell. If that spell is countered
 * this way, exile it instead of putting it into its owner's graveyard. You may cast that
 * card without paying its mana cost for as long as it remains exiled.
 */
val KheruSpellsnatcher = card("Kheru Spellsnatcher") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Snake Wizard"
    power = 3
    toughness = 3
    oracleText = "Morph {4}{U}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, counter target spell. If that spell is countered this way, exile it instead of putting it into its owner's graveyard. You may cast that card without paying its mana cost for as long as it remains exiled."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = Targets.Spell
        effect = Effects.CounterSpellToExile(grantFreeCast = true)
    }

    morph = "{4}{U}{U}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e15117f-f05e-4c5d-9ef5-98f21f4d7529.jpg?1562782522"
        ruling("2014-09-20", "You can target a spell you control with Kheru Spellsnatcher's triggered ability.")
        ruling("2014-09-20", "If the card has {X} in its mana cost, you must choose 0 as the value for X when casting it without paying its mana cost.")
    }
}
