package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Imperial Recovery Unit — {2}{W} Artifact — Vehicle 3/4.
 * Whenever this Vehicle attacks, return target creature or Vehicle card with mana value 2 or
 * less from your graveyard to your hand. Crew 2.
 */
class ImperialRecoveryUnitScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    test("attack trigger returns a creature or Vehicle card with mana value 2 or less") {
        val (driver, you, opponent) = setup()

        val bears = driver.putCardInGraveyard(you, "Grizzly Bears")           // creature, MV 2
        val hoverbike = driver.putCardInGraveyard(you, "High-Speed Hoverbike") // Vehicle, MV 2
        val courserInYard = driver.putCardInGraveyard(you, "Centaur Courser")  // creature, MV 3
        val shock = driver.putCardInGraveyard(you, "Shock")                    // instant, MV 1
        val opposingBears = driver.putCardInGraveyard(opponent, "Grizzly Bears")

        val crew = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val unit = driver.putPermanentOnBattlefield(you, "Imperial Recovery Unit")
        driver.removeSummoningSickness(unit)

        driver.submitSuccess(CrewVehicle(you, unit, listOf(crew)))
        driver.bothPass()
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(DeclareAttackers(you, mapOf(unit to opponent)))

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets[0].shouldNotBeNull()
        legal shouldContainExactlyInAnyOrder listOf(bears, hoverbike)
        legal shouldNotContain courserInYard
        legal shouldNotContain shock
        legal shouldNotContain opposingBears

        driver.submitTargetSelection(you, listOf(hoverbike))
        driver.bothPass()

        driver.getHand(you) shouldContain hoverbike
        driver.getGraveyard(you) shouldContain bears
        driver.getGraveyard(you).contains(hoverbike) shouldBe false
    }

    test("with no legal card in the graveyard the trigger does nothing") {
        val (driver, you, opponent) = setup()

        val courserInYard = driver.putCardInGraveyard(you, "Centaur Courser")
        val crew = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val unit = driver.putPermanentOnBattlefield(you, "Imperial Recovery Unit")
        driver.removeSummoningSickness(unit)
        val handBefore = driver.getHandSize(you)

        driver.submitSuccess(CrewVehicle(you, unit, listOf(crew)))
        driver.bothPass()
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submitSuccess(DeclareAttackers(you, mapOf(unit to opponent)))
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe false
        driver.bothPass()

        driver.getHandSize(you) shouldBe handBefore
        driver.getGraveyard(you) shouldContain courserInYard
    }
})
