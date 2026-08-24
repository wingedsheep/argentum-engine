package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Power Matrix
 * {4}
 * Artifact
 *
 * One composite over the shared target: the +1/+1 pump plus one grant per keyword, each defaulting
 * to `Duration.EndOfTurn`.
 */
val PowerMatrix = card("Power Matrix") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Target creature gets +1/+1 and gains flying, first strike, and trample until end of turn."

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", TargetCreature())
        effect = Effects.Composite(
            Effects.ModifyStats(1, 1, t),
            Effects.GrantKeyword(Keyword.FLYING, t),
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, t),
            Effects.GrantKeyword(Keyword.TRAMPLE, t)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "309"
        artist = "Alan Pollack"
        flavorText = "As he repaired the ship's planeswalking engine, Karn saw for the first time how he could catalyze the *Weatherlight*'s evolution."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a578599c-7d90-4881-b59a-9cf64b90d917.jpg"
    }
}
