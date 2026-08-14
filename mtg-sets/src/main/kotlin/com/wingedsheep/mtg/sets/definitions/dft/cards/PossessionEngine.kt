package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Possession Engine — Aetherdrift #54
 * {3}{U}{U} · Artifact — Vehicle · 5/5
 *
 * When this Vehicle enters, gain control of target creature an opponent controls for as long as
 * you control this Vehicle. That creature can't attack or block for as long as you control this
 * Vehicle.
 * Crew 3
 *
 * Both halves share the one target and the one duration, so both are
 * [Duration.WhileYouControlSource] anchored to the Vehicle — not `WhileSourceOnBattlefield`. That
 * distinction is the whole point of the wording: if an opponent steals Possession Engine, "you
 * control this Vehicle" stops being true and the stolen creature goes home *and* regains its
 * ability to attack and block, where a battlefield-scoped duration would leave the creature
 * parked under the wrong player until the Vehicle died.
 *
 * The durations are one-way (CR 611.2b): once the Vehicle changes controller or leaves, both
 * effects end for good and regaining it does not restart them — which also means the two halves
 * can never drift apart.
 */
val PossessionEngine = card("Possession Engine") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    oracleText = "When this Vehicle enters, gain control of target creature an opponent controls " +
        "for as long as you control this Vehicle. That creature can't attack or block for as long " +
        "as you control this Vehicle.\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val stolen = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        val whileControlled = Duration.WhileYouControlSource("this Vehicle")
        effect = Effects.Composite(
            Effects.GainControl(stolen, whileControlled),
            Effects.CantAttackOrBlock(stolen, whileControlled),
        )
        description = "When this Vehicle enters, gain control of target creature an opponent " +
            "controls for as long as you control this Vehicle. That creature can't attack or " +
            "block for as long as you control this Vehicle."
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Leroy Steinmann"
        flavorText = "\"Here's a tip: If your ride needs souls for fuel, make sure to always keep one handy.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f206b0a1-50d8-4d53-850d-fb15fd328267.jpg?1783907906"
    }
}
