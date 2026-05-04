package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration

/**
 * Rig for War
 * {1}{R}
 * Instant
 * Target creature gets +3/+0 and gains first strike and reach until end of turn.
 */
val RigForWar = card("Rig for War") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+0 and gains first strike and reach until end of turn."

    spell {
        val target = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(3, 0, target)
            .then(Effects.GrantKeyword(Keyword.FIRST_STRIKE, target, Duration.EndOfTurn))
            .then(Effects.GrantKeyword(Keyword.REACH, target, Duration.EndOfTurn))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Diana Franco"
        flavorText = "\"We build our beamcutters to mince bedrock. Should slice a Eumidian hull with no issues.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3edd0515-dcc4-4cb5-8b54-9c00173d8a6d.jpg?1752947184"
    }
}
