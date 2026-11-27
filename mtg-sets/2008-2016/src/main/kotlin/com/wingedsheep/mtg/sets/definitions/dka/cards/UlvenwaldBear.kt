package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ulvenwald Bear
 * {2}{G}
 * Creature — Bear
 * 2/2
 * Morbid — When this creature enters, if a creature died this turn, put two +1/+1 counters on target creature.
 */
val UlvenwaldBear = card("Ulvenwald Bear") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear"
    oracleText = "Morbid — When this creature enters, if a creature died this turn, put two +1/+1 counters on target creature."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CreatureDiedThisTurn
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Jason A. Engle"
        flavorText = "Out chasing bears\n—Kessig expression meaning \"died in the woods\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e3837a7-854a-440d-93d7-d36f50149346.jpg"
    }
}
