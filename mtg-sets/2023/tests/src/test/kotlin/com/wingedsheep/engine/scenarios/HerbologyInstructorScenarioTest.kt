package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Herbology Instructor // Malady Invoker (MOM #189).
 *
 *   Front — 1/3; ETB gain 3 life; "{6}{B/P}: Transform. Activate only as a sorcery."
 *   Back  — 3/3; on transforming, target creature an opponent controls gets -0/-X (X = its power).
 */
class HerbologyInstructorScenarioTest : ScenarioTestBase() {

    private val transformAbility get() = cardRegistry.getCard("Herbology Instructor")!!.activatedAbilities[0].id

    init {
        context("Herbology Instructor") {
            test("entering gains 3 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Herbology Instructor")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Herbology Instructor").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                (game.findPermanent("Herbology Instructor") != null) shouldBe true
                game.getLifeTotal(1) shouldBe 23
            }

            test("transforms and gives an opposing creature -0/-3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Herbology Instructor", summoningSickness = false)
                    .withCardOnBattlefield(2, "Giant Spider")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val instructor = game.findPermanent("Herbology Instructor")!!
                val spider = game.findPermanent("Giant Spider")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = instructor, abilityId = transformAbility)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                if (game.getPendingDecision() is ChooseTargetsDecision) game.selectTargets(listOf(spider))
                game.resolveStack()

                game.state.getEntity(instructor)!!.get<CardComponent>()!!.name shouldBe "Malady Invoker"
                game.state.projectedState.getPower(spider) shouldBe 2
                game.state.projectedState.getToughness(spider) shouldBe 1
            }
        }
    }
}
