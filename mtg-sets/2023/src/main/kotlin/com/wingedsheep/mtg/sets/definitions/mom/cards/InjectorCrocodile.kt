package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Injector Crocodile
 * {4}{B}{B}
 * Creature — Phyrexian Crocodile
 * 5/5
 *
 * When this creature dies, incubate 3.
 * Swampcycling {2}
 */
val InjectorCrocodile = card("Injector Crocodile") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Crocodile"
    oracleText = "When this creature dies, incubate 3. (Create an Incubator token with three +1/+1 counters on it " +
        "and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)\n" +
        "Swampcycling {2} ({2}, Discard this card: Search your library for a Swamp card, reveal it, put it into " +
        "your hand, then shuffle.)"
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Incubate(3)
    }

    keywordAbility(KeywordAbility.typecycling("Swamp", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "329"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/974ddcab-bbe0-4bae-9c99-1fd5a5554d2e.jpg?1783916903"
    }
}
