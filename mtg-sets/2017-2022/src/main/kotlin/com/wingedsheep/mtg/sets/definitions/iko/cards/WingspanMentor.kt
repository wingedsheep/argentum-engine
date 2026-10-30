package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wingspan Mentor — Ikoria: Lair of Behemoths #72
 * {2}{U} · Creature — Human Wizard · 1/3
 *
 * When this creature enters, put a flying counter on target non-Human creature you control.
 * {2}{U}, {T}: Put a +1/+1 counter on each creature you control with flying.
 *
 * The flying counter is a keyword counter, so the creature it lands on keeps flying for good and
 * is then counted by the activated ability, whose "with flying" filter reads projected keywords.
 */
val WingspanMentor = card("Wingspan Mentor") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, put a flying counter on target non-Human creature you control.\n" +
        "{2}{U}, {T}: Put a +1/+1 counter on each creature you control with flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl().notSubtype(Subtype.HUMAN)))
        effect = Effects.AddCounters(CounterType.FLYING, 1, creature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl()),
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Sidharth Chaturvedi"
        flavorText = "Their bond grew over time, as did the height of their jumps."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89348a02-29f6-40b9-b39f-cb5f8c230a02.jpg"
    }
}
