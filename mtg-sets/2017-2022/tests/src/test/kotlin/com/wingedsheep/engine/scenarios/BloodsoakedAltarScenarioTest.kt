package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bloodsoaked Altar (M20 #90) — {4}{B}{B} Artifact.
 *
 *   {T}, Pay 2 life, Discard a card, Sacrifice a creature: Create a 5/5 black Demon creature token
 *   with flying. Activate only as a sorcery.
 *
 * Proves the four-part cost is all paid and the sorcery-speed restriction.
 */
class BloodsoakedAltarScenarioTest : ScenarioTestBase() {

    init {
        context("Bloodsoaked Altar") {

            fun builder() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bloodsoaked Altar")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Hill Giant")
                .withActivePlayer(1)

            fun activate(game: TestGame) = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Bloodsoaked Altar")!!,
                    abilityId = cardRegistry.getCard("Bloodsoaked Altar")!!.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(
                        discardedCards = listOf(game.findCardsInHand(1, "Hill Giant").single()),
                        sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!)
                    )
                )
            )

            test("pays 2 life, discards, sacrifices a creature, and creates a 5/5 flying Demon") {
                val game = builder().inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

                withClue("activation should succeed") { activate(game).error shouldBe null }
                game.resolveStack()

                withClue("all costs paid") {
                    game.getLifeTotal(1) shouldBe 18
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                val demons = game.findPermanents("Demon Token")
                demons shouldHaveSize 1
                val demon = demons.single()
                withClue("the token is a 5/5 flyer") {
                    game.state.projectedState.getPower(demon) shouldBe 5
                    game.state.projectedState.getToughness(demon) shouldBe 5
                    game.state.projectedState.hasKeyword(demon, Keyword.FLYING) shouldBe true
                }
            }

            test("can't be activated outside a main phase") {
                val game = builder().inPhase(Phase.COMBAT, Step.BEGIN_COMBAT).build()

                activate(game).error shouldNotBe null
                game.getLifeTotal(1) shouldBe 20
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
            }
        }
    }
}
