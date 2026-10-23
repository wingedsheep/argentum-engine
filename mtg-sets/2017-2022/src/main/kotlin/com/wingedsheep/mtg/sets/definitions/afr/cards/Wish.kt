package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wish — Adventures in the Forgotten Realms #166 (canonical printing)
 * {2}{R} · Sorcery
 *
 * You may play a card you own from outside the game this turn.
 *
 * "Outside the game" is the owner's sideboard (CR 400.11a). The grant covers every card in it, and
 * `maxPlays = 1` makes it one card: the first one played revokes the permission for the rest. The
 * card is chosen as it is played, not as Wish resolves, and is paid for and timed normally.
 */
val Wish = card("Wish") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "You may play a card you own from outside the game this turn."

    spell {
        effect = Effects.Pipeline {
            val outside = gather(CardSource.FromZone(Zone.SIDEBOARD, Player.You))
            run(Effects.GrantMayPlayFromExile(
                from = outside,
                expiry = MayPlayExpiry.EndOfTurn,
                maxPlays = 1
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Ekaterina Burmak"
        flavorText = "\"If you had just one wish, what would it be? Gold, or angel song? Finding a lost love, or " +
            "finding your way back home? Let me sing a song about one wish that came true . . .\"\n—Ellywick Tumblestrum"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ed021d2-e2bc-44b3-8934-4bd02e0a42ec.jpg?1783926471"
    }
}
