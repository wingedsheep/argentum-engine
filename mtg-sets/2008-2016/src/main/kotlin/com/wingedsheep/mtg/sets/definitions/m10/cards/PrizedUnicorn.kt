package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustBeBlocked

/**
 * Prized Unicorn
 * {3}{G}
 * Creature — Unicorn
 * 2/2
 * All creatures able to block this creature do so.
 *
 * The Lure clause is the static `MustBeBlocked(allCreatures = true)` (Breaker of Armies, Nessian Boar),
 * which only binds creatures that can block it — a tapped creature or one whose block carries a
 * cost stays free.
 */
val PrizedUnicorn = card("Prized Unicorn") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Unicorn"
    power = 2
    toughness = 2
    oracleText = "All creatures able to block this creature do so."

    staticAbility {
        ability = MustBeBlocked(allCreatures = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Sam Wood"
        flavorText = "\"The desire for its magic horn inspires such bloodthirsty greed that all who see the " +
            "unicorn will kill to possess it.\"\n—Dionus, elvish archdruid"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cde88869-5202-43e9-b0eb-4e40a50d311e.jpg?1783942359"
        ruling(
            "2019-07-12",
            "If a creature the defending player controls can't block Prized Unicorn for any reason (such as " +
                "being tapped), then it doesn't block Prized Unicorn. If there's a cost associated with having " +
                "a creature block Prized Unicorn, the defending player isn't forced to pay that cost, so it " +
                "doesn't have to block."
        )
        ruling(
            "2019-07-12",
            "If more than one Prized Unicorn is attacking, creatures that could block must block one Prized " +
                "Unicorn, but they don't all have to block the same Prized Unicorn."
        )
    }
}
