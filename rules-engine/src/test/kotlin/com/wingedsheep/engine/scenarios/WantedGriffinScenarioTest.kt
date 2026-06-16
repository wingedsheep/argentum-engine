package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wanted Griffin (OTJ #38) — {3}{W} 3/2 Creature — Griffin.
 *
 *   "Flying
 *    When this creature dies, create a 1/1 red Mercenary creature token with
 *    '{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.'"
 *
 * Verifies the Griffin has flying and that when it dies it makes a single 1/1 red Mercenary token.
 */
class WantedGriffinScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Wanted Griffin") {

            test("has flying; dying creates a 1/1 red Mercenary token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wanted Griffin")
                    .withCardInHand(1, "Pyroclasm") // 2 damage to all creatures — lethal to the 3/2
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val griffin = game.findPermanent("Wanted Griffin")!!
                withClue("Wanted Griffin has flying") {
                    projector.getProjectedKeywords(game.state, griffin).contains(Keyword.FLYING) shouldBe true
                }

                fun ownedTokens() = game.state.getBattlefield().filter {
                    val e = game.state.getEntity(it)
                    e?.has<TokenComponent>() == true &&
                        e.get<ControllerComponent>()?.playerId == game.player1Id
                }
                ownedTokens().size shouldBe 0

                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.resolveStack() // Pyroclasm kills the Griffin, then the dies trigger resolves

                withClue("The Griffin died") {
                    (game.findPermanent("Wanted Griffin") == null) shouldBe true
                }

                val tokens = ownedTokens()
                withClue("Dies trigger creates exactly one Mercenary token") { tokens.size shouldBe 1 }
                val token = tokens.first()
                projector.getProjectedPower(game.state, token) shouldBe 1
                projector.getProjectedToughness(game.state, token) shouldBe 1
                val tokenCard = game.state.getEntity(token)!!.get<CardComponent>()!!
                tokenCard.colors shouldBe setOf(Color.RED)
                tokenCard.typeLine.subtypes.map { it.value }.contains("Mercenary") shouldBe true
            }
        }
    }
}
