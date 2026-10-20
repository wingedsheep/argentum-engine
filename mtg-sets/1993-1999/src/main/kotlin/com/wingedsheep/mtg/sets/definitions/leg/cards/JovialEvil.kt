package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jovial Evil
 * {2}{B}
 * Sorcery
 * Jovial Evil deals X damage to target opponent, where X is twice the number of white creatures
 * that player controls.
 */
val JovialEvil = card("Jovial Evil") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Jovial Evil deals X damage to target opponent, where X is twice the number of white creatures that player controls."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.TargetOpponent, GameObjectFilter.Creature.withColor(Color.WHITE)).count() * 2,
            opponent
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Christopher Rush"
        flavorText = "\"Today, for a lark, let's visit the plains. I'm sure we'll find something to entertain us.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c993c74c-a574-423b-81c8-96b0a7a6e529.jpg?1783948064"
    }
}
