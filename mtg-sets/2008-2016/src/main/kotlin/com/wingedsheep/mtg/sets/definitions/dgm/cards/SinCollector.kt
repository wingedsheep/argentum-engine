package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sin Collector
 * {1}{W}{B}
 * Creature — Human Cleric
 * 2/1
 * When this creature enters, target opponent reveals their hand. You choose an instant or
 * sorcery card from it and exile that card.
 *
 * Thought-Knot Seer's `Patterns.Hand.revealHandAndExileChosen` with the filter narrowed to
 * instant-or-sorcery (Nimble Larcenist's shape without the artifact half).
 */
val SinCollector = card("Sin Collector") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, target opponent reveals their hand. You choose an " +
        "instant or sorcery card from it and exile that card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.revealHandAndExileChosen(
            target = opponent,
            filter = GameObjectFilter.InstantOrSorcery,
            prompt = "Choose an instant or sorcery card to exile",
        )
        description = "When this creature enters, target opponent reveals their hand. You choose " +
            "an instant or sorcery card from it and exile that card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Mike Bierek"
        flavorText = "\"That must weigh heavily on your soul. Let me purge it for you.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/0/305a3feb-df49-486c-a3b4-ff2721d60019.jpg?1783940022"
        ruling("2013-04-15", "No player can do anything between you choosing a card and that card being exiled. Notably, your opponent can't cast the card once Sin Collector's ability has started to resolve.")
    }
}
