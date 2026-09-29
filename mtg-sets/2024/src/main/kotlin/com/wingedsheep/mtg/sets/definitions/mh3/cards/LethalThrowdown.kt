package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

val LethalThrowdown = card("Lethal Throwdown") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature or sacrifice a modified creature. (Equipment, Auras you control, and counters are modifications.)\nDestroy target creature or planeswalker. If the modified creature was sacrificed, draw a card."

    additionalCost(Costs.additional.Choice(
        Costs.additional.SacrificePermanent(GameObjectFilter.Creature),
        Costs.additional.SacrificePermanent(GameObjectFilter.Creature.copy(
            statePredicates = listOf(StatePredicate.IsModified)
        )),
        choiceSlot = ChoiceSlot.ADDITIONAL_COST_BRANCH,
    ))
    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(victim) then Effects.If(
            Conditions.CastChoiceIs(ChoiceSlot.ADDITIONAL_COST_BRANCH, "1"),
            Effects.DrawCards(1),
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Aaron J. Riley"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/657f52a0-f6fb-4ad8-8d50-c8209de95ab8.jpg?1783911279"
    }
}
