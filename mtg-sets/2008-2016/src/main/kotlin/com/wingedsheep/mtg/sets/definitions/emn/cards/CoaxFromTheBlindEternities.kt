package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Coax from the Blind Eternities
 * {2}{U}
 * Sorcery — Rare (EMN #51)
 *
 * "You may reveal an Eldrazi card you own from outside the game or choose a face-up Eldrazi card
 * you own in exile. Put that card into your hand."
 *
 * Implementation — the same one-choice-across-two-pools shape as Karn, the Great Creator's −2:
 *  - "Outside the game" is the private [Zone.SIDEBOARD] (the wish model); the exile half is your
 *    own exile. Both are gathered into one pool so the player makes a single choice.
 *  - The filter is any card with the Eldrazi subtype, face up. Per ruling, a face-down exiled card
 *    has no characteristics and can't be chosen even if you may look at it; devoid cards without
 *    the Eldrazi subtype don't qualify either.
 *  - "You may" is the up-to-one pick: declining is choosing zero, and the spell still resolves.
 *    The chosen card goes to hand revealed.
 */
val CoaxFromTheBlindEternities = card("Coax from the Blind Eternities") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "You may reveal an Eldrazi card you own from outside the game or choose a face-up " +
        "Eldrazi card you own in exile. Put that card into your hand."

    spell {
        effect = Effects.Pipeline {
            val pool = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.SIDEBOARD, Zone.EXILE),
                    player = Player.You,
                    filter = GameObjectFilter.Any.withSubtype("Eldrazi").faceUp(),
                ),
            )
            val chosen = chooseUpTo(
                1, from = pool,
                prompt = "You may choose an Eldrazi card from outside the game or a face-up one in exile",
            )
            toHand(chosen, revealed = true)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Jaime Jones"
        flavorText = "At Nahiri's call, Emrakul traversed the vast emptiness between the planes and " +
            "arrived on Innistrad."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/740ad768-2be0-4e5a-9d60-6c7ec4bdc107.jpg?1783937505"

        ruling("2016-07-13", "In a casual game, a card you choose from outside the game comes from your personal collection. In a tournament event, a card you choose from outside the game must come from your sideboard.")
        ruling("2016-07-13", "A card that is exiled face down has no characteristics. You can't choose it, even if you're allowed to look at it.")
        ruling("2016-07-13", "An Eldrazi card is one with the subtype Eldrazi. Noncreature cards with devoid from the Battle for Zendikar block are Eldrazi-themed, but they are not Eldrazi cards.")
    }
}
