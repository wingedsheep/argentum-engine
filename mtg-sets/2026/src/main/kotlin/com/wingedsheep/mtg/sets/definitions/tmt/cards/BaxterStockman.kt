package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Step

/**
 * Baxter Stockman
 * {3}{U}{R}
 * Legendary Creature — Human Scientist
 * 3/3
 *
 * When Baxter Stockman enters, create a 1/1 colorless Robot artifact
 * creature token.
 * At the beginning of combat on your turn, target artifact creature
 * you control gets +3/+0 and gains first strike and vigilance until
 * end of turn.
 */
val BaxterStockman = card("Baxter Stockman") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Scientist"
    oracleText = "When Baxter Stockman enters, create a 1/1 colorless Robot artifact creature token.\nAt the beginning of combat on your turn, target artifact creature you control gets +3/+0 and gains first strike and vigilance until end of turn."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(),
            creatureTypes = setOf("Robot"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/0/8/08497fc5-1c0e-4c3c-a356-bf4b34bd4c45.jpg?1771590585"
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(
            TargetFilter(GameObjectFilter.ArtifactCreature.youControl()),
        )
        effect = Effects.ModifyStats(3, 0, creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.VIGILANCE, creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Randy Gallegos"
        flavorText = "\"You flatter me, Ms. O'Neil. I like that.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/1/117b1341-2cf0-466e-b3a8-7e1afa42cd4c.jpg?1771586994"
    }
}
