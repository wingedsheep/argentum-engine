package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chakram Retriever — Battlebond #15 (canonical printing)
 * {4}{U} · Creature — Elemental Dog · 2/4
 *
 * Partner with Chakram Slinger (When this creature enters, target player may put Chakram Slinger
 * into their hand from their library, then shuffle.)
 * Whenever you cast a spell during your turn, untap target creature.
 *
 * "Partner with" is two abilities (CR 702.124j): the deck-construction pairing
 * ([KeywordAbility.partnerWith]) and an enters trigger the card writes out itself — "target player
 * may search their library for a card named Chakram Slinger, reveal it, put it into their hand, then
 * shuffle." The targeted player makes the choice and does the searching, so the search runs inside
 * [Effects.ForEachPlayer] over that player (Path to Exile's "its controller may search" shape).
 */
val ChakramRetriever = card("Chakram Retriever") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Dog"
    power = 2
    toughness = 4
    oracleText = "Partner with Chakram Slinger (When this creature enters, target player may put " +
        "Chakram Slinger into their hand from their library, then shuffle.)\n" +
        "Whenever you cast a spell during your turn, untap target creature."

    keywordAbility(KeywordAbility.partnerWith("Chakram Slinger"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.ForEachPlayer(
            player.asPlayer,
            listOf(
                Effects.May(
                    Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Any.named("Chakram Slinger"),
                        count = 1,
                        destination = SearchDestination.HAND,
                        reveal = true,
                        shuffleAfter = true
                    )
                )
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts()
        triggerRestriction = Conditions.IsYourTurn
        val creature = target(TargetFilter.Creature)
        effect = Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b57d518c-21bb-4451-8248-9ee838460c09.jpg?1783934874"
        ruling(
            "2018-06-08",
            "\"Partner with [name]\" represents two abilities. The first is a triggered ability: \"When " +
                "this permanent enters the battlefield, target player may search their library for a " +
                "card named [name], reveal it, put it into their hand, then shuffle their library.\""
        )
        ruling(
            "2018-06-08",
            "Note that the target player searches their library (which may be affected by effects " +
                "such as that of Stranglehold) and that the card they find is revealed, even though " +
                "these words aren't included in the ability's reminder text."
        )
        ruling(
            "2018-06-08",
            "Chakram Retriever's last ability resolves before the spell that caused it to trigger."
        )
    }
}
