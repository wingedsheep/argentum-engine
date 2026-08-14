package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity

/**
 * Stromkirk Occultist — Eldritch Moon #146.
 * {2}{R}
 * Creature — Vampire Horror
 * 3/2
 *
 * Trample
 * Whenever this creature deals combat damage to a player, exile the top card of your library.
 * Until end of turn, you may play that card.
 * Madness {1}{R}
 *
 * The combat-damage payoff is plain impulse draw ([Patterns.Exile.impulse]) — the exiled card is
 * played for its normal cost, and the permission lapses at end of turn whether or not it was used.
 */
val StromkirkOccultist = card("Stromkirk Occultist") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Horror"
    power = 3
    toughness = 2
    oracleText = "Trample\n" +
        "Whenever this creature deals combat damage to a player, exile the top card of your " +
        "library. Until end of turn, you may play that card.\n" +
        "Madness {1}{R} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Patterns.Exile.impulse(count = 1)
    }

    madness("{1}{R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fa6a443-b23b-48ae-97bb-8fd4deec52c4.jpg?1783937451"
        ruling("2022-12-08", "A card with madness that's discarded counts as having been discarded even though it's put into exile rather than a graveyard. If it was discarded to pay a cost, that cost is still paid. Abilities that trigger when a card is discarded will still trigger.")
        ruling("2022-12-08", "Casting a spell with madness ignores the timing rules based on the card's card type. For example, you can cast a sorcery with madness if you discard it during an opponent's turn.")
    }
}
