package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Riverwise Augur — Rivals of Ixalan #48
 * {3}{U} · Creature — Merfolk Wizard 2/2
 *
 * When this creature enters, draw three cards, then put two cards from your hand on top of your
 * library in any order.
 *
 * The enters trigger is Brainstorm's pipeline (as on Cavalier of Gales).
 */
val RiverwiseAugur = card("Riverwise Augur") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, draw three cards, then put two cards from your hand on top of " +
        "your library in any order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(3) then
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                val putBack = chooseExactly(2, hand)
                toLibraryTop(putBack)
            }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Alayna Danner"
        flavorText = "\"Time flows as the rivers do, toward an unknown sea. The wise know to look downstream.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ea5ac8f-f83d-47ca-a20e-3d129afb34d2.jpg?1783935321"
    }
}
