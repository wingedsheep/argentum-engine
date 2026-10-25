package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Wedding Ring (VOC #32).
 *
 * Proves the cast-only ETB hands the targeted opponent a token copy (which, never cast, doesn't
 * hand one back), and that the mirrored draw / life-gain triggers fire only for an opponent who
 * controls an artifact named Wedding Ring, and only during that opponent's own turn.
 */
class WeddingRingScenarioTest : ScenarioTestBase() {
    init {
        context("Wedding Ring") {
            test("cast, it gives target opponent a token copy, and the copy doesn't trigger again") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wedding Ring")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wedding Ring").error shouldBe null
                game.resolveStack()
                // The lone opponent is the only legal target, so the trigger auto-targets them.
                game.hasPendingDecision() shouldBe false
                game.resolveStack()

                val rings = game.findPermanents("Wedding Ring")
                rings shouldHaveSize 2
                val token = rings.single { game.state.getEntity(it)!!.has<TokenComponent>() }
                game.state.projectedState.getController(token) shouldBe game.player2Id
                game.hasPendingDecision() shouldBe false
                game.state.stack.isEmpty() shouldBe true
            }

            test("an opponent with a Wedding Ring drawing and gaining life on their turn feeds you") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wedding Ring")
                    .withCardOnBattlefield(2, "Wedding Ring", isToken = true)
                    .withCardInHand(2, "Divination")
                    .withCardInHand(2, "Angel's Mercy")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(4) { builder = builder.withCardInLibrary(1, "Plains") }
                repeat(4) { builder = builder.withCardInLibrary(2, "Island") }
                val game = builder.build()

                val handBefore = game.handSize(1)
                game.castSpell(2, "Divination").error shouldBe null
                game.resolveStack()
                withClue("one trigger per card the opponent drew") {
                    game.handSize(1) shouldBe handBefore + 2
                }

                game.castSpell(2, "Angel's Mercy").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 27
                game.getLifeTotal(1) shouldBe 27
            }

            test("an opponent without a Wedding Ring drawing on their turn gives you nothing") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wedding Ring")
                    .withCardInHand(2, "Divination")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(4) { builder = builder.withCardInLibrary(1, "Plains") }
                repeat(4) { builder = builder.withCardInLibrary(2, "Island") }
                val game = builder.build()

                val handBefore = game.handSize(1)
                game.castSpell(2, "Divination").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe handBefore
            }

            test("an opponent with a Wedding Ring gaining life on your turn gives you nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wedding Ring")
                    .withCardOnBattlefield(2, "Wedding Ring", isToken = true)
                    .withCardInHand(2, "Angel's Mercy")
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Angel's Mercy").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 27
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
