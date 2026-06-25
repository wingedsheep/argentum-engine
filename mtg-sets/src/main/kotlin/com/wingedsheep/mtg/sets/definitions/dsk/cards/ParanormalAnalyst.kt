package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect

/**
 * Paranormal Analyst
 * {1}{U}
 * Creature — Human Detective
 * 1/3
 *
 * Whenever you manifest dread, put a card you put into your graveyard this way into your hand.
 *
 * Implementation notes:
 * - Listens for [Triggers.WheneverYouManifestDread] (CR 701.60). Every manifest dread emits a
 *   `ManifestedDreadEvent` carrying the card(s) put into the graveyard this way; the engine seeds
 *   those into the resolving trigger's pipeline under
 *   [IterationSpace.TRIGGER_CAPTURED_COLLECTION] (the same engine-seeded slot Kambal's batch payoff
 *   reads). The payoff is a single [MoveCollectionEffect] moving that captured card out of the
 *   graveyard and into your hand.
 * - Manifest dread puts at most one card into the graveyard, so the captured collection holds 0 or
 *   1 cards — moving the whole collection realises "put **a** card … into your hand". When the
 *   library held fewer than two cards there is nothing to put in the graveyard; the trigger still
 *   fires (CR 701.60b) and the move is a safe no-op on the empty collection.
 */
val ParanormalAnalyst = card("Paranormal Analyst") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Detective"
    power = 1
    toughness = 3
    oracleText = "Whenever you manifest dread, put a card you put into your graveyard this way " +
        "into your hand."

    triggeredAbility {
        trigger = Triggers.WheneverYouManifestDread
        effect = MoveCollectionEffect(
            from = IterationSpace.TRIGGER_CAPTURED_COLLECTION,
            destination = CardDestination.ToZone(Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "James Ryman"
        flavorText = "\"If I've done this right, it should immobilize all spirits in the area. " +
            "If I've done this wrong... you know what, let's just hope I did it right.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60cf954a-5503-460c-8720-8960842eea47.jpg?1726286109"
    }
}
