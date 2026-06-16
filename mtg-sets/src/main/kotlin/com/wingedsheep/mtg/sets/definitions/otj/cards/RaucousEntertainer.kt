package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Raucous Entertainer
 * {1}{G}
 * Creature — Plant Bard
 * 2/2
 *
 * {1}, {T}: Put a +1/+1 counter on each creature you control that entered this turn.
 *
 * The group is restricted to creatures you control with [GameObjectFilter.enteredThisTurn]
 * (backed by the engine's EnteredThisTurnComponent, cleared each turn). Inside the per-member
 * iteration, the counter goes on [EffectTarget.Self] (the iterated creature).
 */
val RaucousEntertainer = card("Raucous Entertainer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Bard"
    power = 2
    toughness = 2
    oracleText = "{1}, {T}: Put a +1/+1 counter on each creature you control that entered this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl().enteredThisTurn()),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
        description = "{1}, {T}: Put a +1/+1 counter on each creature you control that entered this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Forrest Imel"
        flavorText = "It's not hard to know the right song for the occasion when most occasions are saloon brawls."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dc3cbfe-410f-40e3-8021-647a2efb50bf.jpg?1712355978"
    }
}
