package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Aladdin
 * {2}{R}{R}
 * Creature — Human Rogue
 * 1/1
 * {1}{R}{R}, {T}: Gain control of target artifact for as long as you control this creature.
 */
val Aladdin = card("Aladdin") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1
    oracleText = "{1}{R}{R}, {T}: Gain control of target artifact for as long as you control this creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}{R}"), Costs.Tap)
        val artifact = target("target artifact", Targets.Artifact)
        effect = Effects.GainControl(artifact, Duration.WhileSourceOnBattlefield())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Julie Baroh"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db52bad2-a3ec-4f6f-9418-12e8c40703f6.jpg?1562935954"
    }
}
