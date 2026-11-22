package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Deadbridge Chant
 * {4}{B}{G}
 * Enchantment
 * When this enchantment enters, mill ten cards.
 * At the beginning of your upkeep, choose a card at random in your graveyard. If it's a creature
 * card, put it onto the battlefield. Otherwise, put it into your hand.
 *
 * The upkeep line is Gather → Random → Split: the engine draws one card at random from the *whole*
 * graveyard ([chooseRandom] is SelectionMode.Random, no player choice), and only then is that one
 * card split by type — so the creature/non-creature branch reads the card actually chosen, rather
 * than biasing the roll toward either kind. An empty graveyard picks nothing and both moves no-op.
 */
val DeadbridgeChant = card("Deadbridge Chant") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, mill ten cards.\n" +
        "At the beginning of your upkeep, choose a card at random in your graveyard. If it's a " +
        "creature card, put it onto the battlefield. Otherwise, put it into your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(10)
        description = "When this enchantment enters, mill ten cards."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(zone = Zone.GRAVEYARD, player = Player.You))
            val chosen = chooseRandom(1, from = graveyard)
            val (creature, other) = filterSplit(chosen, GameObjectFilter.Creature)
            move(creature, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            toHand(other)
        }
        description = "At the beginning of your upkeep, choose a card at random in your graveyard. " +
            "If it's a creature card, put it onto the battlefield. Otherwise, put it into your hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "63"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26417a58-b0c9-49fa-956c-794ee1c09a4f.jpg?1783940031"
    }
}
