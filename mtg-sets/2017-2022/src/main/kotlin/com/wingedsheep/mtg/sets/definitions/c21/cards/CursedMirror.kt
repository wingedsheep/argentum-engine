package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Cursed Mirror — Commander 2021 #50 (reprinted in Modern Horizons 3)
 * {2}{R} · Artifact
 *
 * {T}: Add {R}.
 * As this artifact enters, you may have it become a copy of any creature on the battlefield until
 * end of turn, except it has haste.
 *
 * The copy is an ordinary as-enters [EntersAsCopy] with [Duration.EndOfTurn]: at cleanup it
 * reverts to the printed artifact, haste and all, so from the next turn on it is a mana rock.
 */
val CursedMirror = card("Cursed Mirror") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{T}: Add {R}.\nAs this artifact enters, you may have it become a copy of any creature on the " +
        "battlefield until end of turn, except it has haste."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    replacementEffect(
        EntersAsCopy(
            optional = true,
            additionalKeywords = listOf(Keyword.HASTE),
            duration = Duration.EndOfTurn,
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/458a01ea-f161-43a4-a6db-88f6418b9c6d.jpg?1783927594"
    }
}
