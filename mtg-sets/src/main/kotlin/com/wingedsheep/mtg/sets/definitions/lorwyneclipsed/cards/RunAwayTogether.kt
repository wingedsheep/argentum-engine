package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

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
 */
val RunAwayTogether = card("Run Away Together") {
    manaCost = "{1}{U}"
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
        collectorNumber = "67"
        artist = "Annie Stegg"
        flavorText = "\"You don't need to steal my dreams, my love. They are already yours.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35c56aff-1f0f-464a-b705-d67803e3d060.jpg?1767732563"
        ruling("2022-06-10", "If both creatures are controlled by the same player as Run Away Together tries to resolve, both targets are illegal. The spell doesn't resolve.")
        ruling("2022-06-10", "If one of the two target creatures becomes an illegal target, Run Away Together can still determine its controller only to check whether the other creature is a legal target. If the illegal target has left the battlefield, use its last known information. If the other creature is still a legal target, it's returned to its owner's hand.")
    }
}
