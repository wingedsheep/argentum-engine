package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.BalthierAndFran
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Balthier and Fran — {1}{R}{G} 4/3 Legendary Creature — Human Rabbit, Reach.
 *
 *  - "Vehicles you control get +1/+1 and have vigilance and reach." (three static abilities)
 *  - "Whenever a Vehicle crewed by Balthier and Fran this turn attacks, if it's the first combat
 *     phase of the turn, you may pay {1}{R}{G}. If you do, after this phase, there is an additional
 *     combat phase."
 *
 * Exercises the two new engine primitives: the source-relative
 * `StatePredicate.CrewedOrSaddledBySourceThisTurn` attack-trigger filter, and the
 * `Conditions.IsFirstCombatPhaseOfTurn` intervening-if. The additional combat phase is asserted via
 * the `AdditionalPhasesComponent` queue that `Effects.AddCombatPhase` appends.
 */
class BalthierAndFranScenarioTest : FunSpec({

    // A plain Crew 1 Vehicle under test (4/4). Crew 1 so Balthier (power 4) alone can crew it.
    val testWagon = card("Test Wagon") {
        manaCost = "{3}"
        typeLine = "Artifact — Vehicle"
        power = 4
        toughness = 4
        oracleText = "Crew 1"
        keywordAbility(KeywordAbility.crew(1))
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BalthierAndFran))
        driver.registerCard(testWagon)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.queuedPhases(player: EntityId): List<ExtraPhaseKind> =
        state.getEntity(player)?.get<AdditionalPhasesComponent>()?.phases ?: emptyList()

    // --- Static lord ---------------------------------------------------------------------------

    test("Vehicles you control get +1/+1 and gain vigilance and reach") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Balthier and Fran")
        val wagon = driver.putPermanentOnBattlefield(me, "Test Wagon")

        // Crew the Vehicle so it becomes a creature and its (buffed) P/T is projected.
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.submitSuccess(CrewVehicle(me, wagon, listOf(bear)))
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(wagon) shouldBe 5      // 4 + 1
        projected.getToughness(wagon) shouldBe 5  // 4 + 1
        projected.hasKeyword(wagon, Keyword.VIGILANCE) shouldBe true
        projected.hasKeyword(wagon, Keyword.REACH) shouldBe true
    }

    test("Balthier and Fran (not a Vehicle) does not get the Vehicle lord buff, but has printed Reach") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val balthier = driver.putCreatureOnBattlefield(me, "Balthier and Fran")

        val projected = driver.state.projectedState
        projected.getPower(balthier) shouldBe 4       // unbuffed
        projected.getToughness(balthier) shouldBe 3
        projected.hasKeyword(balthier, Keyword.REACH) shouldBe true       // printed keyword
        projected.hasKeyword(balthier, Keyword.VIGILANCE) shouldBe false  // not granted to self
    }

    // --- Attack trigger ------------------------------------------------------------------------

    test("paying {1}{R}{G} when a Balthier-crewed Vehicle attacks adds an additional combat phase") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        val balthier = driver.putCreatureOnBattlefield(me, "Balthier and Fran")
        val wagon = driver.putPermanentOnBattlefield(me, "Test Wagon")
        driver.removeSummoningSickness(wagon)

        // Balthier crews the Vehicle → recorded as a crewer on the Vehicle this turn.
        driver.submitSuccess(CrewVehicle(me, wagon, listOf(balthier)))
        driver.bothPass() // resolve the crew ability (Vehicle becomes a creature)

        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(wagon), opp)
        driver.bothPass() // resolve the attack trigger → optional {1}{R}{G} payment

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        if (driver.pendingDecision is SelectManaSourcesDecision) {
            driver.submitManaAutoPayOrDecline(me, autoPay = true)
        }

        // "After this phase, there is an additional combat phase" — one COMBAT phase queued.
        driver.queuedPhases(me) shouldBe listOf(ExtraPhaseKind.COMBAT)
    }

    test("declining the {1}{R}{G} payment adds no additional combat phase") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        val balthier = driver.putCreatureOnBattlefield(me, "Balthier and Fran")
        val wagon = driver.putPermanentOnBattlefield(me, "Test Wagon")
        driver.removeSummoningSickness(wagon)

        driver.submitSuccess(CrewVehicle(me, wagon, listOf(balthier)))
        driver.bothPass()

        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(wagon), opp)
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.queuedPhases(me) shouldBe emptyList()
    }

    test("a Vehicle crewed by a DIFFERENT creature does not trigger Balthier and Fran") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        driver.putCreatureOnBattlefield(me, "Balthier and Fran")
        val wagon = driver.putPermanentOnBattlefield(me, "Test Wagon")
        driver.removeSummoningSickness(wagon)
        // Crew with a bear, NOT with Balthier — so the Vehicle was not crewed by Balthier this turn.
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.submitSuccess(CrewVehicle(me, wagon, listOf(bear)))
        driver.bothPass()

        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(wagon), opp)
        driver.bothPass()

        // No Balthier trigger, hence no optional-payment decision and no queued extra phase.
        (driver.pendingDecision is YesNoDecision) shouldBe false
        driver.queuedPhases(me) shouldBe emptyList()
    }
})
