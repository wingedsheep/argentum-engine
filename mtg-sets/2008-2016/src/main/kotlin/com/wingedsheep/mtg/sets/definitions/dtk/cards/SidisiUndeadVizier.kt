package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity

/**
 * Sidisi, Undead Vizier
 * {3}{B}{B}
 * Legendary Creature — Zombie Snake
 * 4/6
 * Deathtouch
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When Sidisi exploits a creature, you may search your library for a card, put it into your hand,
 * then shuffle.
 *
 * The tutor is an [Effects.May]-gated [Patterns.Library.searchLibrary] (any card, to hand, then
 * shuffle) riding the exploit reflexive, so it still fires when Sidisi exploits herself.
 */
val SidisiUndeadVizier = card("Sidisi, Undead Vizier") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Zombie Snake"
    power = 4
    toughness = 6
    oracleText = "Deathtouch\n" +
        "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When Sidisi exploits a creature, you may search your library for a card, put it into your hand, then shuffle."

    keywords(Keyword.DEATHTOUCH)

    exploit(onExploit = Effects.May(Patterns.Library.searchLibrary()))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ea5dbba-6114-4d97-9363-817ab9e896d3.jpg"
    }
}
