package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AttackerCountLimit
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * "No more than one creature can attack you each combat" (a tapped Mirri, Weatherlight Duelist)
 * caps the whole declaration, which the per-attacker target lists can't show. The AI used to
 * send everything, have the declaration rejected, and attack with nothing; it now trims the plan
 * until the engine accepts it.
 */
class AttackerCountLimitAiTest : FunSpec({

    val warden = card("Test Lone Duelist") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = AttackerCountLimit(1, defendingPlayers = Player.You) }
    }

    test("against a one-attacker cap the AI still attacks, with a single creature") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + warden)
        val (me, opponent) = driver.initMultiplayer(decks = List(2) { Deck.of("Plains" to 40) })
        val attackers = List(3) {
            driver.putCreatureOnBattlefield(me, "Grizzly Bears").also { driver.removeSummoningSickness(it) }
        }
        driver.putPermanentOnBattlefield(opponent, "Test Lone Duelist")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val declare: LegalAction = driver.legalActions(me).single { it.actionType == "DeclareAttackers" }
        val registry = CardRegistry().apply { register(TestCards.all + warden) }
        val advisor = CombatAdvisor(GameSimulator(registry), AIPlayer.defaultEvaluator())
        val plan = advisor.chooseAttackers(driver.state, declare, me) as DeclareAttackers

        plan.attackers shouldHaveSize 1
        (plan.attackers.keys.single() in attackers) shouldBe true
        driver.submitSuccess(plan)
    }
})
