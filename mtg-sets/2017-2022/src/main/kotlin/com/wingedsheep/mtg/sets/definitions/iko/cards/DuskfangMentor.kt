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
 * Duskfang Mentor — Ikoria: Lair of Behemoths #86
 * {2}{B} · Creature — Human Cleric · 1/3
 *
 * When this creature enters, put a lifelink counter on target non-Human creature you control.
 * {1}{B}, {T}: Put a +1/+1 counter on each creature you control with lifelink.
 *
 * Lifelink is a keyword counter: the projected state grants the keyword to a creature carrying a
 * lifelink counter, so the activated ability's "with lifelink" group (read from projected state)
 * sees both counter-granted and printed lifelink.
 */
val DuskfangMentor = card("Duskfang Mentor") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, put a lifelink counter on target non-Human creature you control.\n" +
        "{1}{B}, {T}: Put a +1/+1 counter on each creature you control with lifelink."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype.HUMAN).youControl()))
        effect = Effects.AddCounters(CounterType.LIFELINK, 1, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.LIFELINK).youControl()),
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Livia Prima"
        flavorText = "\"What she sees, I see. What she tastes, I taste.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc38fe5d-6c35-4fff-8f1b-726a0685c6f8.jpg"
    }
}
