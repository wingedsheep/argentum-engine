package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player

/** Tormod's Crypt — sacrifice it to exile target player's graveyard. */
val TormodsCrypt = card("Tormod's Crypt") {
    manaCost = "{0}"
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: Exile target player's graveyard."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        target("target player", Targets.Player)
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                storeAs = "crypt_graveyard",
            ),
            MoveCollectionEffect(
                from = "crypt_graveyard",
                destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0)),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Christopher Rush"
        flavorText = "The dark opening seemed to breathe the cold, damp air of the dead earth in a steady rhythm."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f9668ba-d26d-4484-b4b8-6fb91fbfb617.jpg?1783947925"
    }
}
