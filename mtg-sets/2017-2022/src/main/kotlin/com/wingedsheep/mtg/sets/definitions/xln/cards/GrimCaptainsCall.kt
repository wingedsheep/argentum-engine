package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Grim Captain's Call
 * {2}{B}
 * Sorcery — Uncommon (XLN #108)
 *
 * "Return a Pirate card from your graveyard to your hand, then do the same for Vampire, Dinosaur,
 * and Merfolk."
 *
 * Implementation:
 *  - Nothing is targeted (ruling): each return is a resolution-time `chooseExactly(1)` over a
 *    freshly gathered pool of that subtype, done in printed order.
 *  - Each pool is gathered only after the previous card has moved, so a card with two of these
 *    types (a Vampire Dinosaur) is returned at most once, for whichever type it's picked under,
 *    and another card can be returned for its other type (ruling).
 *  - An empty pool returns nothing and the spell moves on to the next type (ruling); a pool of one
 *    resolves without prompting.
 *  - "Pirate card" is any card with the subtype, not only creatures (ruling: only cards that
 *    actually have the subtype).
 */
val GrimCaptainsCall = card("Grim Captain's Call") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return a Pirate card from your graveyard to your hand, then do the same for " +
        "Vampire, Dinosaur, and Merfolk."

    spell {
        effect = Effects.Pipeline(
            descriptionOverride = "Return a Pirate card from your graveyard to your hand, then do " +
                "the same for Vampire, Dinosaur, and Merfolk."
        ) {
            for (subtype in listOf("Pirate", "Vampire", "Dinosaur", "Merfolk")) {
                val pool = gather(
                    CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Any.withSubtype(subtype))
                )
                val chosen = chooseExactly(
                    1,
                    from = pool,
                    prompt = "Return a $subtype card from your graveyard to your hand",
                    selectedLabel = "Return to hand"
                )
                toHand(chosen)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Winona Nelson"
        flavorText = "When the battle is over, the bitterest foes at last find common ground."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0055934f-1567-4207-aaad-d32d0fe1cdba.jpg?1783935759"
        ruling("2017-09-29", "Grim Captain's Call doesn't target the cards to return. You choose them while it's resolving. No players may take actions between the time you make each choice and the time you return each card to your hand.")
        ruling("2017-09-29", "If a card in your graveyard has more than one of these types (such as a Dinosaur that's a Vampire Dinosaur due to Arcane Adaptation's effect), you may choose to return it for one of its types and return another card of the other type.")
        ruling("2017-09-29", "If you don't have a card in your graveyard of one of the listed creature types, you just continue to the next listed type.")
    }
}
