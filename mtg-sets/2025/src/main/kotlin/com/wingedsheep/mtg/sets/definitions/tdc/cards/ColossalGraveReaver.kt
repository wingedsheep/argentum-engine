package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination

/**
 * Colossal Grave-Reaver
 * {6}{B}{G}
 * Creature — Dragon
 * 7/6
 * Flying
 * Whenever this creature enters or attacks, mill three cards.
 * Whenever one or more creature cards are put into your graveyard from your library, put one of
 * them onto the battlefield.
 *
 * - "Enters or attacks" is two triggered abilities (Sidisi, Brood Tyrant's shape).
 * - The second ability is the library-to-graveyard batch trigger: it fires once per batch (the
 *   ruling — three milled cards land simultaneously) and seeds `triggerCaptured` with exactly the
 *   creature cards that caused it. "One of them" narrows that batch to cards still in the
 *   graveyard (a card moved away in response is a new object, CR 400.7) and the controller picks
 *   exactly one, which enters under their control. With none left, nothing happens.
 */
val ColossalGraveReaver = card("Colossal Grave-Reaver") {
    manaCost = "{6}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Dragon"
    power = 7
    toughness = 6
    oracleText = "Flying\n" +
        "Whenever this creature enters or attacks, mill three cards.\n" +
        "Whenever one or more creature cards are put into your graveyard from your library, " +
        "put one of them onto the battlefield."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Library.mill(3)
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).putIntoYourGraveyard(fromLibrary = true)
        effect = Effects.Pipeline {
            val stillThere = filter(triggerCaptured, GameObjectFilter.Creature.currentlyIn(Zone.GRAVEYARD))
            val chosen = chooseExactly(
                1,
                from = stillThere,
                prompt = "Choose a creature card to put onto the battlefield",
                selectedLabel = "Put onto the battlefield",
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c3f7d76-528a-490a-852a-071adcd7bd55.jpg?1783907165"

        ruling(
            "2025-04-04",
            "All three of the milled cards are put into your graveyard at the same time. If there " +
                "is more than one creature card among those cards, the last ability triggers only once."
        )
    }
}
