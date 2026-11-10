package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unity of Purpose — Oath of the Gatewatch #66 (canonical printing)
 * {3}{U} · Instant
 *
 * Support 2. (Put a +1/+1 counter on each of up to two target creatures.)
 * Untap each creature you control with a +1/+1 counter on it.
 *
 * Support (no "other" clause, any controller) runs first; the untap group is gathered when the second
 * instruction resolves, so a creature you control that just got its counter from support untaps too,
 * while a supported opponent's creature keeps its counter but stays tapped.
 */
val UnityOfPurpose = card("Unity of Purpose") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Support 2. (Put a +1/+1 counter on each of up to two target creatures.)\n" +
        "Untap each creature you control with a +1/+1 counter on it."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl()),
            Effects.Untap(EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Jason Felix"
        flavorText = "Faced with the novel problem of inspiring an army, Jace asked himself: \"What would Gideon say?\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f87292c-0140-44d7-881e-2e8c9ff737a1.jpg?1783937916"
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
        ruling("2016-01-22", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
    }
}
