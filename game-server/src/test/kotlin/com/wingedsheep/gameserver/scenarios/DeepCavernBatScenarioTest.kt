package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Deep-Cavern Bat.
 *
 * Deep-Cavern Bat: {1}{B}
 * Creature — Bat (1/1)
 * Flying, lifelink
 * When this creature enters, look at target opponent's hand. You may exile a
 * nonland card from it until this creature leaves the battlefield.
 */
class DeepCavernBatScenarioTest : ScenarioTestBase() {

    init {
        context("Deep-Cavern Bat") {

            test("ETB exiles a chosen nonland card from target opponent's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Deep-Cavern Bat")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Forest")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Deep-Cavern Bat")
                game.resolveStack() // Bat enters → ETB triggers
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack() // resolve ETB → look at hand → pause for select

                // Controller picks the nonland Hill Giant to exile
                val giantId = game.findCardsInHand(2, "Hill Giant").first()
                game.selectCards(listOf(giantId))

                // Hill Giant should be in opponent's exile, linked to the Bat
                game.state.getExile(game.player2Id).any { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                } shouldBe true
                game.isInHand(2, "Hill Giant") shouldBe false

                val batId = game.findPermanent("Deep-Cavern Bat")!!
                val linked = game.state.getEntity(batId)?.get<LinkedExileComponent>()
                linked shouldNotBe null
                linked!!.exiledIds shouldHaveSize 1
                linked.exiledIds.first() shouldBe giantId

                // Forest (a land) must stay in opponent's hand — only nonland cards are eligible
                game.isInHand(2, "Forest") shouldBe true
            }

            test("LTB returns the exiled card to opponent's hand when the Bat dies") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Deep-Cavern Bat")
                    .withCardInHand(2, "Shock") // opponent will use Shock to kill the Bat
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Deep-Cavern Bat")
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                val giantId = game.findCardsInHand(2, "Hill Giant").first()
                game.selectCards(listOf(giantId))

                // Sanity: Hill Giant exiled
                game.isInHand(2, "Hill Giant") shouldBe false
                val batId = game.findPermanent("Deep-Cavern Bat")!!

                // Opponent kills the Bat with Shock
                game.castSpell(2, "Shock", batId)
                game.resolveStack() // resolve Shock → Bat dies → LTB triggers
                game.resolveStack() // resolve LTB → return linked exile to hand

                game.isOnBattlefield("Deep-Cavern Bat") shouldBe false
                game.isInHand(2, "Hill Giant") shouldBe true
                game.state.getExile(game.player2Id).any { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                } shouldBe false
            }

            test("controller may decline to exile any card") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Deep-Cavern Bat")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Deep-Cavern Bat")
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                // Decline by selecting no cards
                game.selectCards(emptyList())

                // Hill Giant remains in opponent's hand; nothing was exiled
                game.isInHand(2, "Hill Giant") shouldBe true
                game.state.getExile(game.player2Id) shouldHaveSize 0

                val batId = game.findPermanent("Deep-Cavern Bat")!!
                val linked = game.state.getEntity(batId)?.get<LinkedExileComponent>()
                // No linked exile entries when nothing was exiled
                (linked?.exiledIds ?: emptyList()) shouldHaveSize 0
            }
        }
    }
}
