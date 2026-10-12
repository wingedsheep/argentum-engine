package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sibling Rivalry
 * {3}{R}
 * Sorcery
 * Gain control of target artifact or creature until end of turn. Untap it. It gains haste until
 * end of turn. Create a tapped Powerstone token.
 */
val SiblingRivalry = card("Sibling Rivalry") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target artifact or creature until end of turn. Untap it. It gains haste until end of turn. " +
        "Create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    spell {
        val t = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.CreatePowerstone(1, tapped = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c44b24de-60b4-4855-9111-f1237f71bc4d.jpg?1783920061"
    }
}
