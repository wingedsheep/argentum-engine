package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Martyr's Soul
 * {2}{W}
 * Creature — Spirit Soldier
 * 3/2
 * Convoke
 * When this creature enters, if you control no tapped lands, put two +1/+1 counters on it.
 *
 * "If you control no tapped lands" is an intervening-if (CR 603.4): checked when the creature
 * enters and again on resolution.
 */
val MartyrsSoul = card("Martyr's Soul") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Soldier"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\nWhen this creature enters, if you control no tapped lands, put two +1/+1 counters on it."
    power = 3
    toughness = 2
    keywords(Keyword.CONVOKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControl(GameObjectFilter.Land.tapped(), negate = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c17428c-e31d-42f3-8811-6734abd96b0b.jpg?1783933158"
        ruling("2019-06-14", "If you control any tapped lands as Martyr's Soul enters the battlefield, its last ability doesn't trigger at all. If you control any tapped lands as the ability resolves, you won't put two +1/+1 counters on Martyr's Soul.")
    }
}
