package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kappa Cannoneer — {5}{U} Artifact Creature — Turtle Warrior 4/4.
 *
 * Improvise. Ward {4}. Whenever this creature or another artifact you control enters, put a +1/+1
 * counter on this creature. It can't be blocked this turn.
 */
class KappaCannoneerScenarioTest : ScenarioTestBase() {

    init {
        val scrap = card("Cannoneer Scrap") {
            manaCost = "{1}"
            colorIdentity = ""
            typeLine = "Artifact"
            oracleText = ""
        }
        cardRegistry.register(scrap)

        fun TestGame.plusOne(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("improvise pays the generic part; its own entry adds a counter and makes it unblockable this turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Kappa Cannoneer")
                .withCardOnBattlefield(1, "Cannoneer Scrap")
                .withCardOnBattlefield(1, "Cannoneer Scrap")
                .withCardOnBattlefield(1, "Cannoneer Scrap")
                .withCardOnBattlefield(1, "Cannoneer Scrap")
                .withCardOnBattlefield(1, "Cannoneer Scrap")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val scraps = game.findAllPermanents("Cannoneer Scrap")
            val action = game.getLegalActions(1).firstOrNull {
                it.actionType == "CastSpell" && it.action is CastSpell && it.description.contains("Kappa Cannoneer")
            }
            withClue("one Island plus five artifacts can pay {5}{U}") {
                action shouldNotBe null
                action!!.isAffordable shouldBe true
                action.hasTapForGeneric shouldBe true
            }
            val cast = (action!!.action as CastSpell).copy(
                alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = scraps.toSet())
            )
            game.execute(cast).error shouldBe null
            game.resolveStack()

            val kappa = game.findPermanent("Kappa Cannoneer")!!
            scraps.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            withClue("its own entry triggers") {
                game.plusOne(kappa) shouldBe 1
                game.state.projectedState.getPower(kappa) shouldBe 5
                game.state.projectedState.hasKeyword(kappa, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("the evasion lasts only this turn; the counter stays") {
                game.state.projectedState.hasKeyword(kappa, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
                game.plusOne(kappa) shouldBe 1
            }
        }

        test("another artifact entering grows it again; a nonartifact creature does not") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Kappa Cannoneer")
                .withCardInHand(1, "Cannoneer Scrap")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kappa = game.findPermanent("Kappa Cannoneer")!!
            game.plusOne(kappa) shouldBe 0
            game.state.projectedState.hasKeyword(kappa, AbilityFlag.CANT_BE_BLOCKED) shouldBe false

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("a nonartifact creature entering doesn't trigger") {
                game.plusOne(kappa) shouldBe 0
                game.state.projectedState.hasKeyword(kappa, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
            }

            game.castSpell(1, "Cannoneer Scrap").error shouldBe null
            game.resolveStack()
            withClue("another artifact you control entering triggers") {
                game.plusOne(kappa) shouldBe 1
                game.state.projectedState.getPower(kappa) shouldBe 5
                game.state.projectedState.hasKeyword(kappa, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
            }
        }
    }
}
