package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mizzium Tank
 * {1}{R}{R}
 * Artifact — Vehicle
 * 3/2
 * Trample
 * Whenever you cast a noncreature spell, this Vehicle becomes an artifact creature and gets +1/+1
 * until end of turn.
 * Crew 1
 *
 * The cast trigger animates exactly the way crew does: only the CREATURE type is added until end of
 * turn (the Vehicle is already an artifact and keeps its printed 3/2 — no P/T-setting effect), then
 * a +1/+1 layer-7c pump until end of turn on top.
 */
val MizziumTank = card("Mizzium Tank") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Vehicle"
    oracleText = "Trample\n" +
        "Whenever you cast a noncreature spell, this Vehicle becomes an artifact creature and gets " +
        "+1/+1 until end of turn.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 2

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCardType("CREATURE", EffectTarget.Self, Duration.EndOfTurn) then
            Effects.ModifyStats(1, 1, EffectTarget.Self, Duration.EndOfTurn)
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5643253b-0996-45a8-9488-5bcc9192f867.jpg?1783933423"
    }
}
