package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Menacing Ogre
 * {3}{R}{R}
 * Creature — Ogre
 * 3/3
 * Trample, haste
 * When Menacing Ogre enters the battlefield, each player secretly chooses a number.
 * Then those numbers are revealed. Each player with the highest number loses that much
 * life. If you are one of those players, put two +1/+1 counters on Menacing Ogre.
 */
val MenacingOgre = card("Menacing Ogre") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre"
    power = 3
    toughness = 3
    oracleText = "Trample, haste\nWhen this creature enters, each player secretly chooses a number. Then those numbers are revealed. Each player with the highest number loses that much life. If you are one of those players, put two +1/+1 counters on this creature."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val originalController = storePlayer()
            run(Effects.SecretNumbers(
                // Zero is still a highest number when every player chooses zero.
                outcomeMinValue = 0,
                maxValue = DynamicAmounts.fixed(Int.MAX_VALUE),
                highestEffect = Effects.LoseLife(DynamicAmounts.xValue(), EffectTarget.Controller) then
                    Effects.If(
                        condition = Conditions.PlayerIs(Player.You, originalController.asPlayers),
                        then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
                    )
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/5360a871-6932-45b2-bc94-1bd414e38906.jpg?1783945051"
    }
}
