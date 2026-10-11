package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Secret Rendezvous — Strixhaven: School of Mages #26 (canonical printing)
 * {1}{W}{W} · Sorcery
 *
 * You and target opponent each draw three cards.
 */
val SecretRendezvous = card("Secret Rendezvous") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "You and target opponent each draw three cards."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.DrawCards(3) then Effects.DrawCards(3, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Manuel Castañón"
        flavorText = "Strixhaven's five colleges encourage natural rivalries among the students, but some bonds transcend all barriers."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39528cf0-343e-499b-a69f-c5c3c2898c25.jpg?1783927387"
    }
}
