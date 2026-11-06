package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fleetwheel Cruiser — Kaladesh #214 (canonical printing)
 * {4} · Artifact — Vehicle · 5/3
 *
 * Trample, haste
 * When this Vehicle enters, it becomes an artifact creature until end of turn.
 * Crew 2
 *
 * The enters trigger makes the same type change crew does ([Effects.AddCardType] adding CREATURE
 * until end of turn): the Vehicle is already an artifact and keeps its printed 5/3 rather than
 * having its P/T set, and printed haste lets it attack the turn it arrives.
 */
val FleetwheelCruiser = card("Fleetwheel Cruiser") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 3
    oracleText = "Trample, haste\n" +
        "When this Vehicle enters, it becomes an artifact creature until end of turn.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddCardType("CREATURE", EffectTarget.Self, Duration.EndOfTurn)
        description = "When this Vehicle enters, it becomes an artifact creature until end of turn."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Sung Choi"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fdaca1e-5741-456e-ae98-e2c45fd1731b.jpg?1783937156"
        ruling(
            "2017-09-29",
            "When a Vehicle becomes a creature, that doesn't count as having a creature enter the " +
                "battlefield. The permanent was already on the battlefield; it only changed its types. " +
                "Abilities that trigger whenever a creature enters the battlefield won't trigger."
        )
    }
}
