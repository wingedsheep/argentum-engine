package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Unsubstantiate (Eldritch Moon #79)
 * {1}{U} · Instant
 *
 * Return target spell or creature to its owner's hand.
 *
 * Canonical printing: Eldritch Moon, the card's earliest real printing. Core Set 2021 carries a
 * `Printing` row.
 */
val Unsubstantiate = card("Unsubstantiate") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target spell or creature to its owner's hand."

    spell {
        val t = target(TargetSpellOrPermanent(permanentFilter = GameObjectFilter.Creature))
        effect = Effects.ReturnSpellOrPermanentToOwnersHand(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Victor Adame Minguez"
        flavorText = "\"Nicely done, Jace. You always were good at disappearing acts.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba5dac3d-4b49-44c4-a7b2-0a99485252c9.jpg?1783937489"
        ruling("2020-06-23", "If a spell is returned to its owner's hand, it's removed from the stack and thus will not resolve. The spell isn't countered; it just no longer exists. This works against a spell that can't be countered.")
    }
}
