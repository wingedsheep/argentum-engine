package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.por.cards.CoralEel
import com.wingedsheep.mtg.sets.definitions.tmt.cards.TurtleBlimp
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 302.6 / 508.1a — a creature can't attack unless it has been under its controller's control
 * continuously since their most recent turn began. The rule applies to the *permanent*, not just
 * to things that were creatures the entire time, so a Vehicle that just entered this turn and was
 * then crewed must still be summoning-sick for the purposes of attacking.
 *
 * Regression: `ZoneTransitionService` previously gated the `SummoningSicknessComponent` on
 * `cardComponent.typeLine.isCreature`, so non-creature permanents (Vehicles, animated lands, etc.)
 * that became creatures mid-turn could attack the turn they entered.
 */
class VehicleEntersThisTurnAttackTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(TurtleBlimp, CoralEel))
        return driver
    }

    test("Turtle Blimp cast this turn cannot attack after being crewed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }

        // Coral Eel (2/1) is a fine crew partner — power 2 satisfies Crew 2. Summoning sickness
        // on the Eel is irrelevant: the Vehicle's Crew ability taps it as a cost, but that's not
        // the creature activating a {T} ability of its own (CR 302.6 / 602.5a only restrict the
        // creature's own {T}/{Q} costs).
        driver.putCreatureOnBattlefield(you, "Coral Eel")

        // Cast Turtle Blimp from hand so it enters via the real ZoneTransitionService path.
        val blimpInHand = driver.putCardInHand(you, "Turtle Blimp")
        driver.giveMana(you, Color.RED, 5)
        driver.castSpell(you, blimpInHand).isSuccess shouldBe true
        driver.bothPass()  // resolve the spell — Blimp enters, ETB queues the token
        driver.bothPass()  // resolve the ETB trigger — 2/2 Mutant token enters

        val blimpOnBattlefield = driver.findPermanent(you, "Turtle Blimp")
            ?: error("Turtle Blimp not on battlefield after casting")
        val eel = driver.findPermanent(you, "Coral Eel")
            ?: error("Coral Eel not on battlefield")

        // Crew it — Coral Eel's 2 power covers Crew 2. The Vehicle becomes a 3/4 artifact
        // creature until end of turn.
        driver.submitSuccess(CrewVehicle(you, blimpOnBattlefield, listOf(eel)))
        driver.bothPass()  // resolve the Crew activation

        // Advance to the declare-attackers step.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack must be refused: Turtle Blimp hasn't been under its controller's control
        // continuously since the turn began (CR 302.6 / 508.1a).
        driver.submitExpectFailure(
            DeclareAttackers(you, mapOf(blimpOnBattlefield to opponent))
        )
    }
})
