package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bontu the Glorified (AKH #82) — {2}{B} Legendary Creature — God 4/6.
 *
 *   Menace, indestructible
 *   Bontu can't attack or block unless a creature died under your control this turn.
 *   {1}{B}, Sacrifice another creature: Scry 1. Each opponent loses 1 life and you gain 1 life.
 */
class BontuTheGlorifiedScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy {
        cardRegistry.requireCard("Bontu the Glorified").activatedAbilities[0].id
    }

    init {
        context("Bontu the Glorified") {

            fun game() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bontu the Glorified", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.sacrificeBears() {
                val result = execute(
                    ActivateAbility(
                        playerId = player1Id,
                        sourceId = findPermanent("Bontu the Glorified")!!,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(findPermanent("Grizzly Bears")!!)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
                resolveStack()
                // Scry 1: keep the card on top.
                while (hasPendingDecision()) {
                    selectCards(emptyList())
                    resolveStack()
                }
            }

            test("can't attack when no creature died under your control this turn") {
                val game = game()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Bontu the Glorified" to 2)).error shouldNotBe null
            }

            test("sacrificing a creature drains 1 and unlocks Bontu's attack") {
                val game = game()
                game.sacrificeBears()

                withClue("Grizzly Bears was sacrificed") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("each opponent loses 1") { game.getLifeTotal(2) shouldBe 19 }
                withClue("you gain 1") { game.getLifeTotal(1) shouldBe 21 }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("a creature died under your control this turn") {
                    game.declareAttackers(mapOf("Bontu the Glorified" to 2)).error shouldBe null
                }
            }

            test("can't block when no creature died under your control this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bontu the Glorified")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf("Bontu the Glorified" to listOf("Grizzly Bears"))).error shouldNotBe null
            }
        }
    }
}
