package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mobilizer Mech — Kamigawa: Neon Dynasty #65 (canonical printing)
 * {1}{U} · Artifact — Vehicle · 3/4
 *
 * Flying
 * Whenever this Vehicle becomes crewed, up to one other target Vehicle you control becomes an
 * artifact creature until end of turn.
 * Crew 3
 *
 * "Becomes crewed" fires when a crew ability of it resolves (CR 702.122e), not on activation.
 * Like Peacewalker Colossus, the animation only adds the artifact and creature types, so the
 * other Vehicle keeps its printed power and toughness.
 */
val MobilizerMech = card("Mobilizer Mech") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhenever this Vehicle becomes crewed, up to one other target Vehicle you " +
        "control becomes an artifact creature until end of turn.\nCrew 3 (Tap any number of " +
        "creatures you control with total power 3 or more: This Vehicle becomes an artifact " +
        "creature until end of turn.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.becomesCrewed()
        val vehicle = target(
            TargetFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl(),
                excludeSelf = true
            ),
            optional = true
        )
        effect = Effects.AddCardType("Artifact", vehicle, Duration.EndOfTurn) then
            Effects.AddCardType("Creature", vehicle, Duration.EndOfTurn)
        description = "Whenever this Vehicle becomes crewed, up to one other target Vehicle you " +
            "control becomes an artifact creature until end of turn."
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Jonathan Kuo"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf4bb9de-ba7c-4916-b8ed-e2e644c06b58.jpg?1783923900"
    }
}
