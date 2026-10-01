package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.StepChangedEvent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.InAdditionalBeginningPhaseComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for `Effects.AddBeginningPhase` — "there is an additional beginning phase after
 * this phase" — and for the order of phases added at the same point (CR 500.8: the most recently
 * created phase happens first).
 *
 * The inserted beginning phase is a real untap, upkeep and draw step within the *same* turn, and is
 * followed by the end step (or the next queued phase), never by a precombat main phase.
 */
class AdditionalBeginningPhaseTest : FunSpec({

    val secondDawn = card("Second Dawn Test") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.AddBeginningPhase }
    }
    val extraBattle = card("Extra Battle Test") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.AddCombatPhase }
    }
    val battleAndMain = card("Battle And Main Test") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.AddCombatPhase then Effects.AddMainPhase }
    }
    val upkeepPriest = card("Upkeep Priest Test") {
        manaCost = "{R}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = Effects.GainLife(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(secondDawn, extraBattle, battleAndMain, upkeepPriest))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        // Turn 1's draw step is skipped for the starting player (CR 103.8a); stamp a later turn
        // number so every draw step here is an ordinary one.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.replaceState(driver.state.copy(turnNumber = 3))
        return driver
    }

    fun GameTestDriver.castInPostcombatMain(player: EntityId, vararg names: String) {
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
        for (name in names) {
            val spell = putCardInHand(player, name)
            giveMana(player, Color.RED, 1)
            val cast = castSpell(player, spell)
            withClue("casting $name: ${cast.error}") { (cast.outcome is com.wingedsheep.engine.core.Outcome.Done) shouldBe true }
            bothPass() // resolve it
            withClue("$name resolved") { state.stack.isEmpty() shouldBe true }
        }
    }

    /** Steps entered from now until the active player's turn ends, read off [StepChangedEvent]s. */
    fun GameTestDriver.recordStepsThroughTurn(): List<Step> {
        val startPlayer = activePlayer
        val firstEventIndex = events.size
        var guard = 0
        while (activePlayer == startPlayer && guard++ < 400) bothPass()
        // The draw manager re-announces the draw step, so collapse consecutive repeats.
        return events.drop(firstEventIndex).filterIsInstance<StepChangedEvent>().map { it.newStep }
            .takeWhile { it != Step.CLEANUP }
            .fold(listOf<Step>()) { acc, step -> if (acc.lastOrNull() == step) acc else acc + step } + Step.CLEANUP
    }

    test("an additional beginning phase runs untap, upkeep and draw, then goes to the end step") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val turn = driver.state.turnNumber
        driver.putCreatureOnBattlefield(player, "Upkeep Priest Test")
        val land = driver.putLandOnBattlefield(player, "Mountain")

        driver.castInPostcombatMain(player, "Second Dawn Test")
        driver.tapPermanent(land)
        val handBefore = driver.getHand(player).size
        val lifeBefore = driver.getLifeTotal(player)

        // Stop at the inserted upkeep and draw steps to read the state there.
        driver.bothPass()
        driver.state.phase shouldBe com.wingedsheep.sdk.core.Phase.BEGINNING
        driver.currentStep shouldBe Step.UPKEEP
        driver.state.activePlayerId shouldBe player
        driver.state.turnNumber shouldBe turn // same turn, not a new one
        driver.isTapped(land) shouldBe false // the inserted untap step untapped it
        driver.state.getEntity(player)?.has<InAdditionalBeginningPhaseComponent>() shouldBe true

        // The upkeep trigger fires during the inserted upkeep.
        driver.passPriorityUntil(Step.DRAW)
        driver.getLifeTotal(player) shouldBe lifeBefore + 1
        driver.getHand(player).size shouldBe handBefore + 1

        // Leaving the inserted draw step goes to the end step, not a precombat main phase.
        driver.bothPass()
        driver.currentStep shouldBe Step.END
        driver.state.turnNumber shouldBe turn
        driver.state.getEntity(player)?.has<InAdditionalBeginningPhaseComponent>() shouldBe false
    }

    test("the turn's step sequence: no second precombat main, end step after the draw") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.castInPostcombatMain(player, "Second Dawn Test")

        driver.recordStepsThroughTurn() shouldContainExactly listOf(
            Step.UNTAP, Step.UPKEEP, Step.DRAW, Step.END, Step.CLEANUP
        )
        driver.state.getEntity(player)?.has<AdditionalPhasesComponent>() shouldBe false
    }

    test("CR 500.8: a combat phase created after the beginning phase happens first") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.castInPostcombatMain(player, "Second Dawn Test", "Extra Battle Test")

        val steps = driver.recordStepsThroughTurn()
            .filter { it == Step.BEGIN_COMBAT || it == Step.UNTAP || it == Step.END }
        steps shouldContainExactly listOf(Step.BEGIN_COMBAT, Step.UNTAP, Step.END)
    }

    test("CR 500.8: a beginning phase created after the combat phase happens first") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.castInPostcombatMain(player, "Extra Battle Test", "Second Dawn Test")

        val steps = driver.recordStepsThroughTurn()
            .filter { it == Step.BEGIN_COMBAT || it == Step.UNTAP || it == Step.END }
        steps shouldContainExactly listOf(Step.UNTAP, Step.BEGIN_COMBAT, Step.END)
    }

    test("phases one effect creates together keep that effect's order") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.castInPostcombatMain(player, "Second Dawn Test", "Battle And Main Test")

        val steps = driver.recordStepsThroughTurn().filter {
            it == Step.BEGIN_COMBAT || it == Step.POSTCOMBAT_MAIN || it == Step.UNTAP || it == Step.END
        }
        // The later creation (combat followed by main) comes first, in its own order; then the
        // earlier beginning phase.
        steps shouldContainExactly listOf(Step.BEGIN_COMBAT, Step.POSTCOMBAT_MAIN, Step.UNTAP, Step.END)
    }

    test("an inserted combat phase that ends into a beginning phase removes creatures from combat") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val bear = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.removeSummoningSickness(bear)
        // Beginning phase first, then the extra combat: the combat is the later creation and so
        // happens first, ending straight into the beginning phase.
        driver.castInPostcombatMain(player, "Second Dawn Test", "Extra Battle Test")

        driver.bothPass() // leave the natural postcombat main → inserted combat
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bear), opponent)
        driver.passPriorityUntil(Step.UPKEEP)

        driver.state.phase shouldBe com.wingedsheep.sdk.core.Phase.BEGINNING
        driver.state.getEntity(bear)?.has<AttackingComponent>() shouldBe false
        driver.isTapped(bear) shouldBe false // and it untapped in the inserted untap step
    }

    test("an inserted combat phase that ends into another combat phase removes creatures from combat") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val bear = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        driver.removeSummoningSickness(bear)
        driver.castInPostcombatMain(player, "Extra Battle Test", "Extra Battle Test")

        driver.bothPass() // leave the natural postcombat main → first inserted combat
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bear), opponent)
        driver.passPriorityUntil(Step.END_COMBAT)
        driver.bothPass() // → the second inserted combat

        driver.currentStep shouldBe Step.BEGIN_COMBAT
        driver.state.getEntity(bear)?.has<AttackingComponent>() shouldBe false
        driver.state.getEntity(player)?.has<AttackersDeclaredThisCombatComponent>() shouldBe false
    }
})
