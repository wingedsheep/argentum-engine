package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Grafted Identity (MID #57) — {2}{U}{U} Enchantment — Aura.
 *
 *   As an additional cost to cast this spell, sacrifice a creature.
 *   Enchant creature
 *   You control enchanted creature.
 *   Enchanted creature gets +1/+1.
 *
 * First Aura in the corpus with a sacrifice additional cost: the cost must be paid alongside the
 * aura target at cast time, and the aura then steals and pumps the creature it enchants.
 */
class GraftedIdentityScenarioTest : ScenarioTestBase() {

    init {
        context("Grafted Identity") {

            test("sacrifices a creature to steal and pump the enchanted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grafted Identity")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val cardId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grafted Identity"
                }
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(giant)),
                        additionalCostPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(bears)
                        )
                    )
                )
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                withClue("Grizzly Bears is sacrificed as the cost is paid") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                game.resolveStack()

                game.state.projectedState.getController(giant) shouldBe game.player1Id
                game.state.projectedState.getPower(giant) shouldBe 4
                game.state.projectedState.getToughness(giant) shouldBe 4
            }

            test("can't be cast without a creature to sacrifice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grafted Identity")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Grafted Identity", targetId = game.findPermanent("Hill Giant")!!)
                withClue("No creature to sacrifice, so the spell can't be cast") { cast.error shouldNotBe null }
                game.state.projectedState.getController(game.findPermanent("Hill Giant")!!) shouldBe game.player2Id
            }
        }
    }
}
