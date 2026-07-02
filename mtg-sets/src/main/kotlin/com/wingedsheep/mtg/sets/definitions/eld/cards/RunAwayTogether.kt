package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Run Away Together
 * {1}{U}
 * Instant
 *
 * Choose two target creatures controlled by different players.
 * Return those creatures to their owners' hands.
 *
 * In a 2-player game, this is equivalent to targeting one creature
 * you control and one creature an opponent controls.
 *
 * Throne of Eldraine (ELD) is Run Away Together's earliest printing, so the canonical
 * CardDefinition lives here; later sets (CMR, CLB, BLB, FDN, ECL, …) carry `Printing` rows.
 */
val RunAwayTogether = card("Run Away Together") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose two target creatures controlled by different players. Return those creatures to their owners' hands."

    spell {
        val t1 = target("creature you control", Targets.CreatureYouControl)
        val t2 = target("creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.ReturnToHand(t1)
            .then(Effects.ReturnToHand(t2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aeffc3c0-567c-442f-ba06-b7d9617c5789.jpg"
        ruling("2022-06-10", "If both creatures are controlled by the same player as Run Away Together tries to resolve, both targets are illegal. The spell doesn't resolve.")
        ruling("2022-06-10", "If one of the two target creatures becomes an illegal target, Run Away Together can still determine its controller only to check whether the other creature is a legal target. If the illegal target has left the battlefield, use its last known information. If the other creature is still a legal target, it's returned to its owner's hand.")
    }
}
