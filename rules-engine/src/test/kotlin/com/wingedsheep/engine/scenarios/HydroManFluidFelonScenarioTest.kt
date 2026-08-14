package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hydro-Man, Fluid Felon (SPM) — blue-cast pump + the end-step "becomes a non-creature land with
 * '{T}: Add {U}' until your next turn". Pins `BecomeArtifactEffect(cardTypes = setOf("LAND"))` with
 * `UntilYourNextTurn` duration and the granted-activated-ability expiry fix.
 */
class HydroManFluidFelonScenarioTest : FunSpec({

    val testBlueInstant = card("Hydro Test Bolt") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.DealDamage(1, com.wingedsheep.sdk.scripting.targets.EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.TargetOpponent)) }
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(testBlueInstant))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    /** Advance to the active player's *next* precombat main (stepping out via END first). */
    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
    }

    test("casting a blue spell pumps Hydro-Man +1/+1 while he is a creature") {
        val (driver, you) = newGame()
        val hydro = driver.putCreatureOnBattlefield(you, "Hydro-Man, Fluid Felon") // 2/2
        val opponent = driver.state.turnOrder.first { it != you }

        driver.giveMana(you, Color.BLUE, 1)
        val bolt = driver.putCardInHand(you, "Hydro Test Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(opponent)))
        driver.bothPass()
        resolveStack(driver)

        driver.state.projectedState.getPower(hydro) shouldBe 3 // 2 + 1
    }

    test("at your end step Hydro-Man becomes a non-creature land") {
        val (driver, you) = newGame()
        val hydro = driver.putCreatureOnBattlefield(you, "Hydro-Man, Fluid Felon")
        driver.removeSummoningSickness(hydro)
        driver.state.projectedState.isCreature(hydro) shouldBe true

        driver.passPriorityUntil(Step.END)
        resolveStack(driver) // resolve the end-step trigger (untap + become a land)

        driver.state.projectedState.isCreature(hydro) shouldBe false
        driver.state.projectedState.hasType(hydro, "LAND") shouldBe true
    }

    test("the temporary land + granted '{T}: Add {U}' expire on your next turn") {
        val (driver, you) = newGame()
        val hydro = driver.putCreatureOnBattlefield(you, "Hydro-Man, Fluid Felon")
        driver.removeSummoningSickness(hydro)

        // End step: he becomes a non-creature land and gains the granted "{T}: Add {U}".
        driver.passPriorityUntil(Step.END)
        resolveStack(driver)
        driver.state.projectedState.isCreature(hydro) shouldBe false
        driver.state.grantedActivatedAbilities.any { it.entityId == hydro } shouldBe true

        // Out to the opponent's turn — still a land while it isn't yet your next turn.
        advanceToNextTurnMain(driver)
        driver.state.projectedState.isCreature(hydro) shouldBe false
        driver.state.grantedActivatedAbilities.any { it.entityId == hydro } shouldBe true

        // Your next turn: after the untap step the UntilYourNextTurn effects expire — he is a
        // creature again and the granted mana ability is gone.
        advanceToNextTurnMain(driver)
        driver.state.activePlayerId shouldBe you
        driver.state.projectedState.isCreature(hydro) shouldBe true
        driver.state.projectedState.hasType(hydro, "LAND") shouldBe false
        driver.state.grantedActivatedAbilities.any { it.entityId == hydro } shouldBe false
    }
})
