package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.GogoMasterOfMimicry
import com.wingedsheep.mtg.sets.definitions.lea.cards.JayemdaeTome
import com.wingedsheep.mtg.sets.definitions.plc.cards.ProdigalPyromancer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Gogo, Master of Mimicry — {2}{U} Legendary Creature — Wizard 2/4.
 *
 * "{X}{X}, {T}: Copy target activated or triggered ability you control X times. You may choose new
 *  targets for the copies. This ability can't be copied and X can't be 0. (Mana abilities can't be
 *  targeted.)"
 *
 * Exercises the multi-copy generalization of [com.wingedsheep.sdk.dsl.Effects.CopyTargetSpellOrAbility]
 * (`copies = DynamicAmount.XValue`): X independent copies of a chosen ability, per-copy retargeting
 * (CR 707.10c), no-target abilities copied all the same, plus the "X can't be 0" and "this ability
 * can't be copied" constraints.
 */
class GogoMasterOfMimicryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GogoMasterOfMimicry)
        driver.registerCard(ProdigalPyromancer)
        driver.registerCard(JayemdaeTome)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val gogoAbilityId = GogoMasterOfMimicry.activatedAbilities[0].id
    val pyroAbilityId = ProdigalPyromancer.activatedAbilities[0].id
    val tomeAbilityId = JayemdaeTome.activatedAbilities[0].id

    test("copies a targeted ping ability X=2, retargeting each copy to a different creature") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)

        val gogo = driver.putPermanentOnBattlefield(me, "Gogo, Master of Mimicry")
        val pyromancer = driver.putPermanentOnBattlefield(me, "Prodigal Pyromancer")
        driver.removeSummoningSickness(gogo)
        driver.removeSummoningSickness(pyromancer)

        // Three 1/1 targets so a single point of damage is lethal to each.
        val lionA = driver.putPermanentOnBattlefield(opponent, "Savannah Lions")
        val lionB = driver.putPermanentOnBattlefield(opponent, "Savannah Lions")
        val lionC = driver.putPermanentOnBattlefield(opponent, "Savannah Lions")

        // Activate Prodigal Pyromancer's "{T}: deal 1 damage to any target" at lion A.
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = pyromancer,
                abilityId = pyroAbilityId,
                targets = listOf(ChosenTarget.Permanent(lionA))
            )
        )
        val pyroOnStack = driver.getTopOfStack()!!

        // In response, activate Gogo with X=2 targeting that ping ability — copy it twice.
        driver.giveColorlessMana(me, 4) // {X}{X} with X=2 = {4}
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = gogo,
                abilityId = gogoAbilityId,
                targets = listOf(ChosenTarget.Spell(pyroOnStack)),
                xValue = 2,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        // Gogo's ability resolves → pause to retarget copy 1 → aim at lion B.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && driver.stackSize > 0 && guard < 20) {
            driver.bothPass(); guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        // Retargeting copy 1 immediately pauses again for copy 2's targets, so the response leaves a
        // new pending decision (not a fully-resolved success) — assert only that it wasn't rejected.
        driver.submitTargetSelection(me, listOf(lionB)).error shouldBe null

        // Copy 2's retarget prompt is now pending → aim at lion C.
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(me, listOf(lionC)).error shouldBe null

        // Resolve the two copies (B, C) then the original (A).
        guard = 0
        while (driver.stackSize > 0 && guard < 30) {
            driver.bothPass(); guard++
        }

        // All three 1/1s took a lethal point of damage from the three ability instances.
        driver.getCreatures(opponent).size shouldBe 0
    }

    test("copies a no-target ability X=2 without prompting (Jayemdae Tome draws three total)") {
        val driver = createDriver()
        val me = driver.player1

        val gogo = driver.putPermanentOnBattlefield(me, "Gogo, Master of Mimicry")
        val tome = driver.putPermanentOnBattlefield(me, "Jayemdae Tome")
        driver.removeSummoningSickness(gogo)

        val handBefore = driver.getHandSize(me)

        // Activate Jayemdae Tome's "{4}, {T}: Draw a card".
        driver.giveColorlessMana(me, 4)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = tome,
                abilityId = tomeAbilityId,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        val tomeOnStack = driver.getTopOfStack()!!

        // In response, activate Gogo with X=2 targeting the draw ability — copy it twice.
        driver.giveColorlessMana(me, 4)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = gogo,
                abilityId = gogoAbilityId,
                targets = listOf(ChosenTarget.Spell(tomeOnStack)),
                xValue = 2,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        // No prompts (the draw ability has no targets); resolve everything.
        var guard = 0
        while (driver.stackSize > 0 && guard < 30) {
            (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe false
            driver.bothPass(); guard++
        }

        // The copies don't pay the {4} cost (CR 707.10) — three draws land: original + two copies.
        driver.getHandSize(me) shouldBe handBefore + 3
    }

    test("X can't be 0: activating Gogo with X=0 is rejected") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)

        val gogo = driver.putPermanentOnBattlefield(me, "Gogo, Master of Mimicry")
        val pyromancer = driver.putPermanentOnBattlefield(me, "Prodigal Pyromancer")
        driver.removeSummoningSickness(gogo)
        driver.removeSummoningSickness(pyromancer)
        val lion = driver.putPermanentOnBattlefield(opponent, "Savannah Lions")

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = pyromancer,
                abilityId = pyroAbilityId,
                targets = listOf(ChosenTarget.Permanent(lion))
            )
        )
        val pyroOnStack = driver.getTopOfStack()!!

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = gogo,
                abilityId = gogoAbilityId,
                targets = listOf(ChosenTarget.Spell(pyroOnStack)),
                xValue = 0,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess.shouldBeFalse()
    }

    test("this ability can't be copied: a second Gogo makes no copy of the first's ability") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)

        val gogoA = driver.putPermanentOnBattlefield(me, "Gogo, Master of Mimicry")
        val gogoB = driver.putPermanentOnBattlefield(me, "Gogo, Master of Mimicry")
        val pyromancer = driver.putPermanentOnBattlefield(me, "Prodigal Pyromancer")
        driver.removeSummoningSickness(gogoA)
        driver.removeSummoningSickness(gogoB)
        driver.removeSummoningSickness(pyromancer)
        val lion = driver.putPermanentOnBattlefield(opponent, "Savannah Lions")

        // Pyromancer ping on the stack, then Gogo A copies it (X=1).
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = pyromancer,
                abilityId = pyroAbilityId,
                targets = listOf(ChosenTarget.Permanent(lion))
            )
        )
        val pyroOnStack = driver.getTopOfStack()!!

        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = gogoA,
                abilityId = gogoAbilityId,
                targets = listOf(ChosenTarget.Spell(pyroOnStack)),
                xValue = 1,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        val gogoAOnStack = driver.getTopOfStack()!!

        // Gogo A's ability instance is tagged "can't be copied".
        driver.state.getEntity(gogoAOnStack)?.get<CantBeCopiedComponent>() shouldNotBe null

        // Gogo B targets Gogo A's ability (X=1). It's a legal target, but the copy can't be made.
        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = gogoB,
                abilityId = gogoAbilityId,
                targets = listOf(ChosenTarget.Spell(gogoAOnStack)),
                xValue = 1,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        val stackAfterGogoB = driver.stackSize // pyro + gogoA + gogoB = 3

        // Resolve Gogo B's ability — it copies nothing (no retarget prompt, no new stack object).
        driver.bothPass()
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe false
        // Gogo B left the stack and added no copy: exactly one fewer object than before.
        driver.stackSize shouldBe stackAfterGogoB - 1
    }
})
