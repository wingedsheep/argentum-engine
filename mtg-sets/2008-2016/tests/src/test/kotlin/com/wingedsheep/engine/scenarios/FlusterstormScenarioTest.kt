package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cmd.cards.Flusterstorm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Flusterstorm — {U} Instant.
 * Counter target instant or sorcery spell unless its controller pays {1}. Storm.
 *
 * The storm copy carries its own "unless pays {1}" demand: paying for one copy does not answer
 * the original, so the tax stacks per copy.
 */
class FlusterstormScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Flusterstorm))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        return Triple(driver, caster, driver.getOpponent(caster))
    }

    /** Resolve the top of the stack, answering storm retargeting with [retarget] and taxes with [pay]. */
    fun GameTestDriver.drive(retargetPlayer: com.wingedsheep.sdk.model.EntityId, retarget: com.wingedsheep.sdk.model.EntityId, pay: (Int) -> Boolean) {
        var taxPrompts = 0
        var guard = 0
        while (stackSize > 0 && guard++ < 30) {
            when (val d = pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(retargetPlayer, listOf(retarget))
                is YesNoDecision -> submitYesNo(d.playerId, pay(taxPrompts++))
                null -> bothPass()
                else -> error("unexpected decision $d")
            }
        }
    }

    test("storm copy and original each demand {1}; paying only one still counters the spell") {
        val (driver, caster, opponent) = setup()

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, bolt, listOf(opponent)).outcome shouldBe Outcome.Done
        driver.passPriority(caster)

        // Opponent responds with Flusterstorm; one spell (Bolt) was cast before it -> one copy.
        val storm = driver.putCardInHand(opponent, "Flusterstorm")
        driver.giveMana(opponent, Color.BLUE, 1)
        driver.castSpellWithTargets(opponent, storm, listOf(ChosenTarget.Spell(bolt))).outcome shouldBe Outcome.Done

        // Bolt's controller has exactly {1} — enough to pay one tax, not two.
        driver.giveMana(caster, Color.RED, 1)
        driver.drive(opponent, bolt) { true }

        driver.getGraveyardCardNames(caster) shouldContain "Lightning Bolt"
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("paying both taxes lets the spell resolve") {
        val (driver, caster, opponent) = setup()

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, bolt, listOf(opponent)).outcome shouldBe Outcome.Done
        driver.passPriority(caster)

        val storm = driver.putCardInHand(opponent, "Flusterstorm")
        driver.giveMana(opponent, Color.BLUE, 1)
        driver.castSpellWithTargets(opponent, storm, listOf(ChosenTarget.Spell(bolt))).outcome shouldBe Outcome.Done

        driver.giveMana(caster, Color.RED, 2)
        driver.drive(opponent, bolt) { true }

        driver.getLifeTotal(opponent) shouldBe 17
    }
})
