package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leonardo, Cutting Edge
 * {1}{W}
 * Legendary Creature — Mutant Ninja Turtle
 * 1/1
 *
 * Sneak {W} (You may cast this spell for {W} if you also return an unblocked
 * attacker you control to hand during the declare blockers step. He enters
 * tapped and attacking.)
 * Lifelink
 * Whenever you gain life, put a +1/+1 counter on Leonardo.
 */
val LeonardoCuttingEdge = card("Leonardo, Cutting Edge") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Sneak {W} (You may cast this spell for {W} if you also return an unblocked attacker you control to hand during the declare blockers step. He enters tapped and attacking.)\nLifelink\nWhenever you gain life, put a +1/+1 counter on Leonardo."
    power = 1
    toughness = 1

    sneak("{W}")
    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.YouGainLife
        effect = AddCountersEffect(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74c11ee3-19d1-4ad5-a727-1d74c565d6a5.jpg?1769005535"
    }
}
