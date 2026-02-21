package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Reward the Faithful
 * {W}
 * Instant
 * Any number of target players each gain life equal to the greatest mana value
 * among permanents you control.
 */
val RewardTheFaithful = card("Reward the Faithful") {
    manaCost = "{W}"
    typeLine = "Instant"
    oracleText = "Any number of target players each gain life equal to the greatest mana value among permanents you control."

    spell {
        target("players", TargetPlayer(count = 2, optional = true))
        effect = ForEachTargetEffect(
            listOf(
                GainLifeEffect(
                    amount = DynamicAmounts.battlefield(Player.You).maxManaValue(),
                    target = EffectTarget.ContextTarget(0)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Matt Cavotta"
        flavorText = "\"When you have drunk your fill, pass the cup to others who thirst.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df6e8844-3736-4fb1-bedb-6a6bfa6ccdc8.jpg?1562535753"
    }
}
