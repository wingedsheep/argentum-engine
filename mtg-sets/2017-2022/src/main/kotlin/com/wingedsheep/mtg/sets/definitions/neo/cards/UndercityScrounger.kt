package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Undercity Scrounger — Kamigawa: Neon Dynasty #126 (canonical printing)
 * {2}{B} · Artifact Creature — Human Rogue · 1/4
 *
 * {T}: Create a Treasure token. Activate only if a creature died this turn.
 */
val UndercityScrounger = card("Undercity Scrounger") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Human Rogue"
    power = 1
    toughness = 4
    oracleText = "{T}: Create a Treasure token. Activate only if a creature died this turn. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.CreateTreasure()
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.CreatureDiedThisTurn))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Mike Jordana"
        flavorText = "\"That will pay for another arm extension all by itself! Well, as long as it doesn't explode.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6319b84b-8a3d-4bc8-af48-8b500f124be1.jpg?1783923874"
    }
}
