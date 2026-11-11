package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithExileCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Ulamog's Despoiler
 * {6}
 * Creature — Eldrazi Processor
 * 5/5
 * As this creature enters, you may put two cards your opponents own from exile into their owners'
 * graveyards. If you do, this creature enters with four +1/+1 counters on it.
 */
val UlamogsDespoiler = card("Ulamog's Despoiler") {
    manaCost = "{6}"
    typeLine = "Creature — Eldrazi Processor"
    power = 5
    toughness = 5
    oracleText = "As this creature enters, you may put two cards your opponents own from exile into their " +
        "owners' graveyards. If you do, this creature enters with four +1/+1 counters on it."

    replacementEffect(
        EntersWithExileCounters(
            filter = GameObjectFilter.Any.ownedByOpponent(),
            sourceZone = Zone.EXILE,
            maxCards = DynamicAmounts.fixed(2),
            countersPerCard = 2,
            anyPlayersZone = true,
            destination = Zone.GRAVEYARD,
            allOrNone = true
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55b6b7f0-d7fc-46b6-99ff-ba8206ecd628.jpg?1783938222"
        ruling("2015-08-25", "If a spell or ability requires that you put more than one exiled card into the graveyard, you may choose cards owned by different opponents. Each card chosen will be put into its owner's graveyard.")
        ruling("2015-08-25", "If a replacement effect will cause cards that would be put into a graveyard from anywhere to be exiled instead (such as the one created by Anafenza, the Foremost), you can still put an exiled card into its opponent's graveyard. The card becomes a new object and remains in exile. In this situation, you can't use a single exiled card if required to put more than one exiled card into the graveyard. Conversely, you could use the same card in this situation if two separate spells or abilities each required you to put a single exiled card into its owner's graveyard.")
        ruling("2015-08-25", "You can't look at face-down cards in exile unless an effect allows you to.")
        ruling("2015-08-25", "Face-down cards in exile are grouped using two criteria: what caused them to be exiled face down and when they were exiled face down. If you want to put a face-down card in exile into its owner's graveyard, you must first choose one of these groups and then choose a card from within that group at random. For example, say an artifact causes your opponent to exile their hand of three cards face down. Then on a later turn, that artifact causes your opponent to exile another two cards face down. If you use Wasteland Strangler to put one of those cards into their graveyard, you would pick the first or second pile and put a card chosen at random from that pile into the graveyard.")
    }
}
