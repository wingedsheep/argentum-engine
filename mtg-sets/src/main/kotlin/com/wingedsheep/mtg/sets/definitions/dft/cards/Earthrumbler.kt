package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Earthrumbler — Aetherdrift #160
 * {4}{G} · Artifact — Vehicle · 7/6
 *
 * Vigilance, trample
 * Exile an artifact or creature card from your graveyard: This Vehicle becomes an artifact creature
 * until end of turn.
 * Crew 3
 *
 * A self-crewing Vehicle: the graveyard-exile ability animates it without tapping any creatures.
 * The animation is the same Layer-4 Creature type grant crew resolves to ([Effects.AddCardType]
 * with [Duration.EndOfTurn]) — a Vehicle is already an artifact (CR 301.7) and keeps its printed
 * 7/6 and keywords, so the type change is the whole effect. The cost has no mana component and no
 * activation limit, so it can be paid repeatedly (harmlessly — the extra animations are redundant)
 * as long as the graveyard supplies material.
 */
val Earthrumbler = card("Earthrumbler") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Vehicle"
    oracleText = "Vigilance, trample\n" +
        "Exile an artifact or creature card from your graveyard: This Vehicle becomes an artifact " +
        "creature until end of turn.\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 7
    toughness = 6

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.ExileFromGraveyard(1, GameObjectFilter.CreatureOrArtifact)
        effect = Effects.AddCardType("Creature", EffectTarget.Self, Duration.EndOfTurn)
        description = "Exile an artifact or creature card from your graveyard: This Vehicle becomes " +
            "an artifact creature until end of turn."
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "J.P. Targete"
        flavorText = "The Brood aspires to become speed itself."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee386cd0-934a-4b33-9db3-0a9033ab577e.jpg?1783907872"
    }
}
