package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Horde Ambusher
 * {1}{R}
 * Creature — Human Berserker
 * 2/2
 * Whenever Horde Ambusher blocks, it deals 1 damage to you.
 * Morph—Reveal a red card in your hand.
 * When Horde Ambusher is turned face up, target creature can't block this turn.
 */
val HordeAmbusher = card("Horde Ambusher") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Human Berserker"
    power = 2
    toughness = 2
    oracleText = "Whenever Horde Ambusher blocks, it deals 1 damage to you.\nMorph—Reveal a red card in your hand. (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Horde Ambusher is turned face up, target creature can't block this turn."

    triggeredAbility {
        trigger = Triggers.Blocks
        effect = Effects.DealDamage(1, EffectTarget.Controller)
    }

    morphCost = PayCost.RevealCard(filter = GameObjectFilter.Any.withColor(Color.RED))

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val creature = target("creature", Targets.Creature)
        effect = CantBlockEffect(target = creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53926b92-adf0-4bb2-873f-103d9a1bdda8.jpg?1562786709"
    }
}
