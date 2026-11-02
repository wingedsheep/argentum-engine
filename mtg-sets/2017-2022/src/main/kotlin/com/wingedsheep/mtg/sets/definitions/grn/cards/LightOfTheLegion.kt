package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Light of the Legion
 * {4}{W}{W}
 * Creature — Angel
 * 5/5
 *
 * Flying
 * Mentor
 * When this creature dies, put a +1/+1 counter on each white creature you control.
 */
val LightOfTheLegion = card("Light of the Legion") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)\n" +
        "When this creature dies, put a +1/+1 counter on each white creature you control."

    keywords(Keyword.FLYING)
    mentor()

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.WHITE).youControl()),
            effect = Effects.AddCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                count = 1,
                target = EffectTarget.IterationEntity
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Alex Konstad"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2f1b831-ca55-4575-9dfa-c3163e3d789d.jpg?1783934197"
    }
}
