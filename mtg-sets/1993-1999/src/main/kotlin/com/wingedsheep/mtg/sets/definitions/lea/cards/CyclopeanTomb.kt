package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val CyclopeanTomb = card("Cyclopean Tomb") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Put a mire counter on target non-Swamp land. That land is a Swamp for as long as it has a mire counter on it. Activate only during your upkeep.\nWhen this artifact is put into a graveyard from the battlefield, at the beginning of each of your upkeeps for the rest of the game, remove all mire counters from a land that a mire counter was put onto with this artifact but that a mire counter has not been removed from with this artifact."
    val mire = CounterType.MIRE
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val land = target(TargetFilter.Land.notSubtype(Subtype("Swamp")))
        effect = Effects.Pipeline {
            val affected = gather(CardSource.ChosenTargets)
            run(Effects.IfYouDo(
                action = Effects.AddCounters(mire, 1, land),
                then = Effects.RecordSourceObjects(affected, "marked"),
                successCriterion = SuccessCriterion.CountersAdded
            ))
            run(Effects.SetLandType("Swamp", land, Duration.WhileAffectedHasCounter(mire)))
        }
        restrictions = listOf(ActivationRestriction.DuringStep(Step.UPKEEP), ActivationRestriction.OnlyDuringYourTurn)
        description = "{2}, {T}: Put a mire counter on target non-Swamp land. It is a Swamp while it has a mire counter. Activate only during your upkeep."
    }
    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            fireOnPlayer = com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller,
            repeatAtEachMatchingStep = true,
            expiry = DelayedTriggerExpiry.Never,
            effect = Effects.Pipeline {
                val marked = gather(CardSource.SourceLinkedBattlefield("marked"))
                val cleaned = gather(CardSource.SourceLinkedBattlefield("cleaned"))
                val eligible = exclude(marked, cleaned)
                val land = chooseExactly(1, eligible, filter = GameObjectFilter.Land,
                    prompt = "Choose a land marked by this Cyclopean Tomb to remove all mire counters",
                    useTargetingUI = true)
                run(Effects.ForEachInCollection(land, Effects.IfYouDo(
                    action = Effects.RemoveAllCountersOfType(mire, land.asTarget),
                    then = Effects.RecordSourceObjects(land, "cleaned"),
                    successCriterion = SuccessCriterion.CountersRemoved
                )))
            }
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/894c5cf2-8ae2-427a-bcbc-67df0bdfee9d.jpg?1783948668"
        ruling("2008-08-01", "The land remains a Swamp as long as it has a mire counter on it. This effect is not tied to the Tomb remaining on the battlefield.")
        ruling("2006-10-15", "Will not add or remove the supertype snow to or from a land.")
    }
}
