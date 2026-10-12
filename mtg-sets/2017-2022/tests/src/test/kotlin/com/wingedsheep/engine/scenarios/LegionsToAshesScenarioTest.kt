package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Legions to Ashes (BRO #215) — "Exile target nonland permanent an opponent controls and all
 * tokens that player controls with the same name as that permanent."
 *
 * Pins the group's three scopes: same name, tokens only, and controlled by the target's
 * controller — plus a token target being moved exactly once.
 */
class LegionsToAshesScenarioTest : ScenarioTestBase() {

    init {
        fun board() = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "Legions to Ashes")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("exiles the target and that player's same-named tokens, nothing else") {
            val game = board()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                .withCardOnBattlefield(2, "Hill Giant", isToken = true)
                .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                .build()

            val bears = game.findPermanents("Grizzly Bears")
            val p2Bears = bears.filter { game.state.projectedState.getController(it) == game.player2Id }
            val p1Token = bears.single { game.state.projectedState.getController(it) == game.player1Id }
            val (target, otherNontoken) = p2Bears.filter {
                game.state.getEntity(it)?.get<TokenComponent>() == null
            }
            val p2Tokens = p2Bears.filter { it != target && it != otherNontoken }

            game.castSpell(1, "Legions to Ashes", target).error shouldBe null
            game.resolveStack()

            val battlefield = game.state.getBattlefield().toSet()
            withClue("the nontoken target is exiled") {
                (target in battlefield) shouldBe false
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }
            withClue("the opponent's same-named tokens are exiled") {
                p2Tokens.none { it in battlefield } shouldBe true
            }
            withClue("a same-named nontoken permanent stays") {
                (otherNontoken in battlefield) shouldBe true
            }
            withClue("a differently named token stays") {
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
            withClue("a same-named token another player controls stays") {
                (p1Token in battlefield) shouldBe true
            }
        }

        test("a token target is exiled along with its same-named siblings") {
            val game = board()
                .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                .build()

            val target = game.findPermanents("Grizzly Bears").first()
            game.castSpell(1, "Legions to Ashes", target).error shouldBe null
            game.resolveStack()

            game.findPermanents("Grizzly Bears").size shouldBe 0
        }
    }
}
