package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Harried Artisan // Phyrexian Skyflayer (MOM #143).
 *
 *   Front (2/3) — Haste. "{3}{W/P}: Transform this creature. Activate only as a sorcery."
 *   Back  (3/4) — Flying, haste.
 *
 * Covers the explicit life payment for {W/P} (the client's "pay 2 life" pip toggle) and the flip to
 * a flying, hasty 3/4.
 */
class HarriedArtisanScenarioTest : ScenarioTestBase() {

    init {
        context("Harried Artisan") {

            test("has haste on the front face") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Harried Artisan")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artisan = game.findPermanent("Harried Artisan")!!
                game.state.projectedState.hasKeyword(artisan, Keyword.HASTE) shouldBe true
            }

            test("an explicit 2-life payment for {W/P} transforms it into Phyrexian Skyflayer") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Harried Artisan")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artisan = game.findPermanent("Harried Artisan")!!
                val mountains = game.findAllPermanents("Mountain")
                val plains = game.findPermanent("Plains")!!
                val abilityId = cardRegistry.getCard("Harried Artisan")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = artisan, abilityId = abilityId,
                        paymentStrategy = PaymentStrategy.Explicit(
                            manaAbilitiesToActivate = mountains,
                            phyrexianLifePayments = listOf(Color.WHITE)
                        )
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the pip was paid with 2 life, leaving the Plains untapped") {
                    game.getLifeTotal(1) shouldBe 18
                    game.state.getEntity(plains)!!.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe false
                }
                withClue("transformed into a 3/4 flying, haste Phyrexian Skyflayer") {
                    game.state.getEntity(artisan)!!.get<CardComponent>()!!.name shouldBe "Phyrexian Skyflayer"
                    game.state.projectedState.getPower(artisan) shouldBe 3
                    game.state.projectedState.getToughness(artisan) shouldBe 4
                    game.state.projectedState.hasKeyword(artisan, Keyword.FLYING) shouldBe true
                    game.state.projectedState.hasKeyword(artisan, Keyword.HASTE) shouldBe true
                }
            }
        }
    }
}
