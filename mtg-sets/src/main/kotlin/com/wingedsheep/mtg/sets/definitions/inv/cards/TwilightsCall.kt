package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects

/**
 * Twilight's Call
 * {4}{B}{B}
 * Sorcery
 * You may cast this spell as though it had flash if you pay {2} more to cast it.
 * Each player returns all creature cards from their graveyard to the battlefield.
 *
 * The flash clause is the generalised "pay {N} more to cast as though it had flash"
 * optional additional cost ([KeywordAbility.flashKicker]) — the same shape as Ghitu Fire.
 * The reanimation gathers every creature card in every graveyard (multi-player
 * [Player.Each]) and returns them under their owners' control.
 */
val TwilightsCall = card("Twilight's Call") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You may cast this spell as though it had flash if you pay {2} more to cast it. " +
        "(You may cast it any time you could cast an instant.)\n" +
        "Each player returns all creature cards from their graveyard to the battlefield."

    keywordAbility(KeywordAbility.flashKicker("{2}"))

    spell {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.Each,
                        filter = GameObjectFilter.Creature
                    ),
                    storeAs = "graveyardCreatures"
                ),
                MoveCollectionEffect(
                    from = "graveyardCreatures",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                    underOwnersControl = true
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Mark Romanoski"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c97c8a5-33b3-4f7f-a224-bb4df7b4bcc0.jpg?1562907233"
    }
}
