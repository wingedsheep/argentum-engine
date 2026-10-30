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
 * Frillscare Mentor — Ikoria: Lair of Behemoths #121
 * {2}{R} · Creature — Human Warrior · 3/2
 *
 * When this creature enters, put a menace counter on target non-Human creature you control.
 * (It can't be blocked except by two or more creatures.)
 * {2}{R}, {T}: Put a +1/+1 counter on each creature you control with menace.
 *
 * Menace is a keyword counter: the projected state grants the keyword to a creature carrying a
 * menace counter, so the activated ability's "with menace" group (read from projected state)
 * sees both counter-granted and printed menace.
 */
val FrillscareMentor = card("Frillscare Mentor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, put a menace counter on target non-Human creature you control. " +
        "(It can't be blocked except by two or more creatures.)\n" +
        "{2}{R}, {T}: Put a +1/+1 counter on each creature you control with menace."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype.HUMAN).youControl()))
        effect = Effects.AddCounters(CounterType.MENACE, 1, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.MENACE).youControl()),
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Jason A. Engle"
        flavorText = "Fortune favors the ferocious."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24c7d034-6403-41e2-9e44-4d58ef54cf10.jpg"
    }
}
