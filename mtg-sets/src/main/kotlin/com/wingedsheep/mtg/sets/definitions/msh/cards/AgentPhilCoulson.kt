package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agent Phil Coulson — Marvel Super Heroes #4 (rare)
 * {1}{W} · Legendary Creature — Human Spy Hero · 2/2
 *
 * Vigilance
 * {T}: Put a +1/+1 counter on each other Hero you control.
 *
 * The activated ability is the Shalai, Voice of Plenty group-counter shape: an
 * [Effects.ForEachInGroup] over a [GroupFilter] of Heroes you control with `excludeSelf`
 * (so Coulson — himself a Hero — is skipped, per "each *other* Hero"), whose body puts one
 * +1/+1 counter on the iteration entity ([EffectTarget.Self] inside a group loop).
 * The group is snapshotted before the first counter lands, and the filter is a permanent
 * filter rather than a creature filter so a noncreature Hero permanent would still be
 * counted, matching the oracle wording literally.
 */
val AgentPhilCoulson = card("Agent Phil Coulson") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Spy Hero"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n{T}: Put a +1/+1 counter on each other Hero you control."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.HERO).youControl(),
                excludeSelf = true,
            ),
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Marc Aspinall"
        flavorText = "\"The big guns are in flight. Agent 13, what's your status?\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1383e587-df58-4b45-9067-b9399e90b9ed.jpg?1783902979"
    }
}
