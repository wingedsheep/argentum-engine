package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Return of the Nightstalkers — Portal Second Age #88
 * {5}{B}{B} · Sorcery
 *
 * Return all Nightstalker permanent cards from your graveyard to the battlefield. Then destroy
 * all Swamps you control.
 *
 * Gather-then-move snapshot of the graveyard (Splendid Reclamation shape), followed by a
 * destroy-all over Swamps the controller controls. The destroy is sequenced after the return, so
 * it sees the board the return produced. Swamp is the land subtype, so nonbasic Swamps count.
 */
val ReturnOfTheNightstalkers = card("Return of the Nightstalkers") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return all Nightstalker permanent cards from your graveyard to the battlefield. " +
        "Then destroy all Swamps you control."

    spell {
        effect = Effects.Pipeline {
            val nightstalkers = gather(
                CardSource.FromZone(
                    Zone.GRAVEYARD,
                    Player.You,
                    GameObjectFilter.Permanent.withSubtype(Subtype.NIGHTSTALKER)
                )
            )
            move(nightstalkers, CardDestination.ToZone(Zone.BATTLEFIELD))
        } then Effects.DestroyAll(GameObjectFilter.Land.withSubtype(Subtype.SWAMP).youControl())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e85d85c2-9f34-4375-adcd-a1c5b487cacc.jpg?1783946471"
    }
}
