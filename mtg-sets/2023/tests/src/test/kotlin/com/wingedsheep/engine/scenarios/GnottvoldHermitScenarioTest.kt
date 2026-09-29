package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gnottvold Hermit // Chrome Host Hulk (MOM #188).
 *
 *   Front (4/4) — "{5}{U/P}: Transform this creature. Activate only as a sorcery."
 *   Back  (5/5) — "Whenever this creature attacks, up to one other target creature has base power and
 *                  toughness 5/5 until end of turn."
 */
class GnottvoldHermitScenarioTest : ScenarioTestBase() {

    init {
        context("Gnottvold Hermit") {

            test("transforms paying 2 life, then its attack trigger makes another creature 5/5 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gnottvold Hermit")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hermit = game.findPermanent("Gnottvold Hermit")!!
                val abilityId = cardRegistry.getCard("Gnottvold Hermit")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = hermit, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("{U/P} was paid with 2 life") { game.getLifeTotal(1) shouldBe 18 }
                withClue("transformed into a 5/5 Chrome Host Hulk") {
                    game.state.getEntity(hermit)!!.get<CardComponent>()!!.name shouldBe "Chrome Host Hulk"
                    game.state.projectedState.getPower(hermit) shouldBe 5
                    game.state.projectedState.getToughness(hermit) shouldBe 5
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Chrome Host Hulk" to 2)).error shouldBe null
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the targeted creature has base P/T 5/5 until end of turn") {
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.getToughness(bears) shouldBe 5
                }
            }

            test("can't transform at instant speed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gnottvold Hermit")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hermit = game.findPermanent("Gnottvold Hermit")!!
                val abilityId = cardRegistry.getCard("Gnottvold Hermit")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = hermit, abilityId = abilityId)
                ).error shouldNotBe null
            }
        }
    }
}
