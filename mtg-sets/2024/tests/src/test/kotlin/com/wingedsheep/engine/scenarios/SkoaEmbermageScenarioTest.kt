package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Skoa, Embermage (MH3 #138).
 *
 * When Skoa enters, it deals 4 damage to any target.
 * Grandeur — Discard another card named Skoa, Embermage, Sacrifice two Mountains: Skoa deals 4
 * damage to any target.
 */
class SkoaEmbermageScenarioTest : ScenarioTestBase() {

    private val grandeurId = cardRegistry.getCard("Skoa, Embermage")!!.activatedAbilities[0].id

    init {
        context("Skoa, Embermage") {

            test("ETB deals 4 damage to any target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Skoa, Embermage")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Skoa, Embermage").error shouldBe null
                game.resolveStack()
                withClue("ETB trigger asks for a target") {
                    game.getPendingDecision() shouldNotBe null
                }
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Skoa, Embermage").shouldBeTrue()
                game.getLifeTotal(2) shouldBe 16
            }

            test("Grandeur: discard another Skoa and sacrifice two Mountains to deal 4 damage") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Skoa, Embermage", summoningSickness = false)
                    .withCardInHand(1, "Skoa, Embermage")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val skoa = game.findPermanent("Skoa, Embermage")!!
                val skoaInHand = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Skoa, Embermage"
                }
                val mountains = game.findPermanents("Mountain").take(2)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = skoa,
                        abilityId = grandeurId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(
                            discardedCards = listOf(skoaInHand),
                            sacrificedPermanents = mountains,
                        ),
                    )
                )
                withClue("Grandeur activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 16
                game.isInGraveyard(1, "Skoa, Embermage").shouldBeTrue()
                game.isOnBattlefield("Skoa, Embermage").shouldBeTrue()
                game.findPermanents("Mountain").size shouldBe 1
            }

            test("Grandeur is unaffordable with only one Mountain and a Forest") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Skoa, Embermage", summoningSickness = false)
                    .withCardInHand(1, "Skoa, Embermage")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val skoa = game.findPermanent("Skoa, Embermage")!!
                val grandeur = game.getLegalActions(1).firstOrNull { la ->
                    val a = la.action
                    a is ActivateAbility && a.sourceId == skoa && a.abilityId == grandeurId
                }
                (grandeur?.isAffordable ?: false).shouldBeFalse()
            }

            test("Grandeur is unaffordable without another Skoa in hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Skoa, Embermage", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val skoa = game.findPermanent("Skoa, Embermage")!!
                val grandeur = game.getLegalActions(1).firstOrNull { la ->
                    val a = la.action
                    a is ActivateAbility && a.sourceId == skoa && a.abilityId == grandeurId
                }
                (grandeur?.isAffordable ?: false).shouldBeFalse()
            }
        }
    }
}
