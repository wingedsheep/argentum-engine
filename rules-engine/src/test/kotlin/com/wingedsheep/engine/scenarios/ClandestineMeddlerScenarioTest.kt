package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.ClandestineMeddler
import com.wingedsheep.mtg.sets.definitions.mkm.cards.RepeatOffender
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Clandestine Meddler — "When this creature enters, suspect up to one other target creature you
 * control" + "Whenever one or more suspected creatures you control attack, surveil 1."
 *
 * The two halves are tested separately because they fail in different ways:
 *
 *  1. the ETB's target set has to honour *both* restrictions in "up to one **other** target creature
 *     **you control**" — a filter that dropped either one would still look correct on a board with a
 *     single legal choice, so the test asserts the offered list exactly;
 *  2. the attack trigger is a batch trigger ("one or more"), so it must fire once per combat rather
 *     than once per attacking suspect — the two-suspect attack is what distinguishes them;
 *  3. the trigger must not fire at all when nothing suspected attacks, which is what catches a filter
 *     that fell through to "any creature".
 */
class ClandestineMeddlerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ClandestineMeddler)
        driver.registerCard(RepeatOffender)
        driver.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Meddler and let its enters trigger go on the stack awaiting a target. */
    fun castMeddler(driver: GameTestDriver): EntityId {
        val meddler = driver.putCardInHand(driver.player1, "Clandestine Meddler")
        driver.giveMana(driver.player1, Color.BLACK, 3)
        driver.castSpell(driver.player1, meddler).isSuccess shouldBe true
        driver.bothPass() // resolve the creature; the enters trigger asks for its target
        return meddler
    }

    test("the enters trigger offers only *other* creatures *you* control") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.player2, "Minotaur Warrior")

        castMeddler(driver)

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("the Meddler itself is excluded by 'other', the opponent's by 'you control'") {
            decision.legalTargets.getValue(0) shouldContainExactly listOf(mine)
        }
    }

    test("the chosen creature becomes suspected") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        castMeddler(driver)
        driver.submitTargetSelection(driver.player1, listOf(mine))
        driver.bothPass() // resolve the trigger

        val projected = StateProjector().project(driver.state)
        withClue("suspect grants the status, and with it menace and can't-block (CR 701.60a)") {
            projected.isSuspected(mine) shouldBe true
            projected.cantBlock(mine) shouldBe true
        }
    }

    test("attacking with two suspected creatures surveils once, not twice") {
        val driver = newDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val offender = driver.putCreatureOnBattlefield(driver.player1, "Repeat Offender")
        listOf(bears, offender).forEach { driver.removeSummoningSickness(it) }

        // Suspect the Offender with its own {2}{B} ability...
        driver.giveMana(driver.player1, Color.BLACK, 3)
        driver.submitSuccess(
            ActivateAbility(driver.player1, offender, RepeatOffender.activatedAbilities.first().id)
        )
        driver.bothPass()
        // ...and the Bears via the Meddler's enters trigger, so two suspects attack together.
        castMeddler(driver)
        driver.submitTargetSelection(driver.player1, listOf(bears))
        driver.bothPass()

        val projected = StateProjector().project(driver.state)
        withClue("both attackers must actually be suspected for this to test the batch") {
            projected.isSuspected(bears) shouldBe true
            projected.isSuspected(offender) shouldBe true
        }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(bears, offender), driver.player2)
        driver.bothPass() // resolve the attack trigger

        val surveil = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("surveil 1 looks at exactly one card") {
            surveil.options.size shouldBe 1
        }
        // Put it in the graveyard, then confirm no *second* surveil is waiting behind it.
        driver.submitCardSelection(surveil.playerId, surveil.options)
        withClue("'one or more' is a batch trigger — one surveil for the whole attack") {
            (driver.pendingDecision is SelectCardsDecision) shouldBe false
        }
        driver.getGraveyard(driver.player1).size shouldBe 1
    }

    test("attacking with nothing suspected does not surveil") {
        val driver = newDriver()
        val plain = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(plain)

        castMeddler(driver)
        // Decline the "up to one" — nothing on the board becomes suspected.
        driver.submitTargetSelection(driver.player1, emptyList())
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(plain), driver.player2)

        withClue("an unsuspected attacker must not satisfy the suspected-only filter") {
            (driver.pendingDecision is SelectCardsDecision) shouldBe false
            driver.getGraveyard(driver.player1).size shouldBe 0
        }
    }
})
