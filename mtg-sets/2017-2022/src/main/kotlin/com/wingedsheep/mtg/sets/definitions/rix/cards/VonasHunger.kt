package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vona's Hunger
 * {2}{B}
 * Instant
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * Each opponent sacrifices a creature of their choice. If you have the city's blessing, instead each
 * opponent sacrifices half the creatures they control of their choice, rounded up.
 *
 * Spell ascend is checked as the spell resolves (Golden Demise's shape). The blessed mode reads each
 * opponent's own creature count (`countPerPlayer` binds [Player.You] to the sacrificing opponent), and
 * every opponent's sacrifice is one simultaneous event.
 */
val VonasHunger = card("Vona's Hunger") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "Each opponent sacrifices a creature of their choice. If you have the city's blessing, instead each " +
        "opponent sacrifices half the creatures they control of their choice, rounded up."

    keywords(Keyword.ASCEND)

    spell {
        val eachOpponent = EffectTarget.PlayerRef(Player.EachOpponent)
        effect = Effects.If(
            condition = Conditions.ControlPermanentsAtLeast(10),
            then = Effects.GainCitysBlessing()
        ) then Effects.If(
            condition = Conditions.YouHaveCitysBlessing,
            then = Effects.Sacrifice(
                filter = GameObjectFilter.Creature,
                count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count() divRoundedUp 2,
                target = eachOpponent,
                countPerPlayer = true
            ),
            otherwise = Effects.Sacrifice(GameObjectFilter.Creature, 1, eachOpponent)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aab958d5-9c1f-420f-b358-ec0325f22f84.jpg?1783935304"
    }
}
