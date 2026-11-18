package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Profane Memento — Magic 2015 #226
 * {1} · Artifact · Uncommon
 *
 * Whenever a creature card is put into an opponent's graveyard from anywhere, you gain 1 life.
 *
 * "From anywhere" is a zone change with `to = GRAVEYARD` and no `from`. The graveyard is keyed by
 * owner (a card always goes to its owner's graveyard), so `ownedByOpponent()` — which also makes a
 * stolen creature an opponent owns trigger it when it dies, per the ruling. Tokens aren't cards, so
 * [CardPredicate.IsNontoken] keeps a dying token creature from triggering it.
 */
val ProfaneMemento = card("Profane Memento") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a creature card is put into an opponent's graveyard from anywhere, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature
                .withCardPredicate(CardPredicate.IsNontoken)
                .ownedByOpponent()
        ).changesZone(to = Zone.GRAVEYARD)
        effect = Effects.GainLife(1)
        description = "Whenever a creature card is put into an opponent's graveyard from anywhere, you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Franz Vohwinkel"
        flavorText = "\"An angel's skull is left too plain by death. I made a few aesthetic modifications.\"\n" +
            "—Dommique, blood artist"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2c97632-3cf1-4b79-9d18-d8991654dcca.jpg?1783939156"
        ruling("2014-07-18", "Token creatures dying won't cause Profane Memento's ability to trigger.")
        ruling(
            "2014-07-18",
            "Profane Memento's ability will trigger when a nontoken creature you control but an opponent owns dies."
        )
        ruling(
            "2014-07-18",
            "If Profane Memento is put into your graveyard at the same time as a creature card is put into an " +
                "opponent's graveyard, its ability won't trigger."
        )
    }
}
