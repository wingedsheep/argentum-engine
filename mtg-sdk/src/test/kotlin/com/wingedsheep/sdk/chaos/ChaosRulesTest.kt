package com.wingedsheep.sdk.chaos

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.serialization.CardSerialization
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Chaos house variant's rule table ([ChaosRules]): one "choose one at random" modal upkeep
 * trigger whose modes are the rules, each labelled with its name.
 */
class ChaosRulesTest : FunSpec({

    test("every rule has its own id and name, and none is also listed as not automated") {
        val ids = ChaosRules.all.map { it.id }
        ids.toSet().size shouldBe ids.size
        val names = ChaosRules.all.map { it.name }
        names.toSet().size shouldBe names.size
        ids.toSet().intersect(ChaosRules.notAutomated.keys) shouldBe emptySet()
    }

    test("the roll draws one rule at random and never one this emblem has drawn before") {
        val roll: ModalEffect = ChaosRules.roll
        roll.chooseAtRandom shouldBe true
        roll.excludePreviouslyChosenModes shouldBe true
        roll.chooseCount shouldBe 1
        withClue("a chaos roll is not a modal spell") { roll.countsAsModalSpell shouldBe false }
        roll.modes shouldBe ChaosRules.all.map { it.mode }
    }

    test("the emblem's ability triggers at the beginning of its controller's upkeep") {
        val trigger = ChaosRules.upkeepRoll.trigger
        trigger shouldBe EventPattern.StepEvent(Step.UPKEEP, Player.You)
        ChaosRules.upkeepRoll.effect shouldBe ChaosRules.roll
        ChaosRules.upkeepRoll.description shouldBe ChaosRules.EMBLEM_TEXT
    }

    test("a rolled rule reads as its name and text wherever the engine describes it") {
        for (rule in ChaosRules.all) {
            val label = "${rule.name} — ${rule.text}"
            withClue(rule.name) {
                rule.mode.description shouldBe label
                (rule.mode.effect as CompositeEffect).descriptionOverride shouldBe label
                rule.mode.effect.description shouldBe label
            }
        }
    }

    test("the emblem's ability round-trips through card JSON") {
        val json = CardSerialization.json
        val encoded = json.encodeToString(TriggeredAbility.serializer(), ChaosRules.upkeepRoll)
        json.decodeFromString(TriggeredAbility.serializer(), encoded) shouldBe ChaosRules.upkeepRoll
    }

    test("named finds a rule, and an unknown name fails loudly") {
        ChaosRules.named("Time Walk").id shouldBe 61
        val failure = runCatching { ChaosRules.named("No Such Rule") }.exceptionOrNull()
        (failure is IllegalStateException) shouldBe true
    }
})
