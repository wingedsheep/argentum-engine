package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Crenellated Wall
 * {4}
 * Artifact Creature — Wall
 * 0 / 4
 */
val CrenellatedWall = card("Crenellated Wall") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Wall"
    oracleText = "Defender (This creature can't attack.)\n" +
        "{T}: Target creature gets +0/+4 until end of turn."
    power = 0
    toughness = 4

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", Targets.Creature)
        effect = Effects.ModifyStats(0, 4, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "290"
        artist = "Arnie Swekel"
        flavorText = "Mercadian soldiers excel at finding things to stand behind."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d85ad08d-1120-411a-8bbe-ac93a56476bd.jpg"
    }
}
