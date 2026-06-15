package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ErthaJoFrontierMentor
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ertha Jo, Frontier Mentor — {2}{R}{W} Legendary Creature — Kor Advisor 2/4
 *
 * "When Ertha Jo enters, create a 1/1 red Mercenary creature token with '{T}: Target creature you
 *  control gets +1/+0 until end of turn. Activate only as a sorcery.'
 *  Whenever you activate an ability that targets a creature or player, copy that ability. You may
 *  choose new targets for the copy."
 *
 * Exercises the new `Triggers.youActivateAbilityTargeting(AbilityTargetMatch.CreatureOrPlayer)`
 * trigger + matcher: the copy fires only when the activated ability targets a creature or player,
 * and the copy reprompts for new targets (CR 707.10 / 707.10c) so a second creature can be hit.
 */
class ErthaJoFrontierMentorScenarioTest : FunSpec({

    // Helper: a creature with a creature-targeting activated ability ("{T}: target creature you
    // control gets +1/+0"). Used to drive Ertha Jo's copy trigger deterministically.
    val pumpBuddy = card("Pump Buddy") {
        manaCost = "{1}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
        oracleText = "{T}: Target creature you control gets +1/+0 until end of turn."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0))
            target = Targets.CreatureYouControl
            timing = TimingRule.InstantSpeed
        }
    }

    // Helper: a creature with a NON-targeting activated ability ("{T}: you gain 1 life").
    // Activating it must NOT fire Ertha Jo's copy trigger.
    val lifeBuddy = card("Life Buddy") {
        manaCost = "{1}"
        typeLine = "Creature — Cleric"
        power = 1
        toughness = 1
        oracleText = "{T}: You gain 1 life."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = Effects.GainLife(1)
            timing = TimingRule.InstantSpeed
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ErthaJoFrontierMentor)
        driver.registerCard(pumpBuddy)
        driver.registerCard(lifeBuddy)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("Ertha Jo's ETB creates a 1/1 red Mercenary token") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // Cast Ertha Jo so the enters-the-battlefield trigger actually fires.
        val ertha = driver.putCardInHand(me, "Ertha Jo, Frontier Mentor")
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.RED, 1)
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.WHITE, 1)
        driver.castSpell(me, ertha)
        driver.bothPass() // resolve Ertha Jo -> ETB trigger on stack
        driver.bothPass() // resolve ETB trigger -> create token

        val mercenaries = driver.getCreatures(me).filter { driver.getCardName(it) == "Mercenary Token" }
        mercenaries.size shouldBe 1
        val merc = mercenaries.single()
        driver.state.projectedState.getPower(merc) shouldBe 1
        driver.state.projectedState.getToughness(merc) shouldBe 1
    }

    test("activating a creature-targeting ability copies it and a second creature can be pumped") {
        val driver = createDriver()
        val me = driver.player1

        // Ertha Jo's copy trigger is a battlefield triggered ability, so she just needs to be in
        // play — no ETB needed here (the Mercenary token is irrelevant; Pump Buddy drives the test).
        driver.putCreatureOnBattlefield(me, "Ertha Jo, Frontier Mentor")

        val buddy = driver.putCreatureOnBattlefield(me, "Pump Buddy")
        driver.removeSummoningSickness(buddy)

        // Two other creatures to pump: creatureA (original target) and creatureB (copy's new target).
        val creatureA = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val creatureB = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val pumpAbilityId = driver.cardRegistry.requireCard("Pump Buddy").activatedAbilities[0].id

        // Activate "{T}: target creature you control gets +1/+0" targeting creatureA.
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = buddy,
                abilityId = pumpAbilityId,
                targets = listOf(ChosenTarget.Permanent(creatureA))
            )
        )

        // Ertha Jo's trigger goes on the stack above the original ability and copies it; the copy
        // executor pauses for the "choose new targets" prompt. Aim the copy at creatureB.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(me, listOf(creatureB)).isSuccess shouldBe true

        // Resolve everything (the copy, then the original ability).
        guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 20) {
            driver.bothPass()
            guard++
        }

        // Both creatures got +1/+0 (original ability -> A, copy -> B).
        driver.state.projectedState.getPower(creatureA) shouldBe 3
        driver.state.projectedState.getPower(creatureB) shouldBe 3
        driver.state.projectedState.getToughness(creatureA) shouldBe 2
        driver.state.projectedState.getToughness(creatureB) shouldBe 2
    }

    test("activating a non-targeting ability does NOT trigger the copy") {
        val driver = createDriver()
        val me = driver.player1

        driver.putCreatureOnBattlefield(me, "Ertha Jo, Frontier Mentor")

        val lifeGuy = driver.putCreatureOnBattlefield(me, "Life Buddy")
        driver.removeSummoningSickness(lifeGuy)

        val lifeBefore = driver.getLifeTotal(me)
        val gainAbilityId = driver.cardRegistry.requireCard("Life Buddy").activatedAbilities[0].id

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = lifeGuy,
                abilityId = gainAbilityId
            )
        )

        // No copy trigger should appear; resolve the stack.
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 20) {
            (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe false
            driver.bothPass()
            guard++
        }

        // The ability resolved exactly once: +1 life, not +2.
        driver.getLifeTotal(me) shouldBe lifeBefore + 1
    }
})
