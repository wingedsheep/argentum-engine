package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Street Spasm (DDJ #30) — {X}{R} Instant, Overload {X}{X}{R}{R}.
 *
 * The overload cost carries *two* {X}: X=2 costs {4}{R}{R}, yet the spell deals X (2), not 2X,
 * to each creature without flying you don't control. Nothing else lowers a doubled X in an
 * overload cost, so this pins both the price and the resolved amount.
 */
class StreetSpasmScenarioTest : ScenarioTestBase() {

    private fun TestGame.castOverloaded(xValue: Int): ExecutionResult {
        val cardId = state.getHand(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Street Spasm"
        }
        return execute(
            CastSpell(
                player1Id, cardId,
                xValue = xValue,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.OVERLOAD
            )
        )
    }

    init {
        test("normal cast deals X to the target creature without flying") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Street Spasm")
                .withLandsOnBattlefield(1, "Mountain", 4) // X=3 → {3}{R}
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val cast = game.castXSpell(1, "Street Spasm", xValue = 3, targetId = giant)
            withClue("cast with X=3 is legal: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("normal cast can't target a flyer") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Street Spasm")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Storm Crow")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val crow = game.findPermanent("Storm Crow")!!
            game.castXSpell(1, "Street Spasm", xValue = 2, targetId = crow).error shouldNotBe null
        }

        test("overload with X=2 pays {4}{R}{R} and deals 2 to each opposing non-flyer") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Street Spasm")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardOnBattlefield(1, "Grizzly Bears") // mine — spared
                .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 — dies to 2
                .withCardOnBattlefield(2, "Hill Giant")    // 3/3 — survives 2 (would die to 2X = 4)
                .withCardOnBattlefield(2, "Storm Crow")    // 1/2 flyer — spared
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castOverloaded(xValue = 2)
            withClue("overloaded cast with X=2 is legal: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("opponent's Grizzly Bears took 2 and died") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
            withClue("my Grizzly Bears is untouched") { game.isInGraveyard(1, "Grizzly Bears") shouldBe false }
            withClue("Hill Giant took X=2, not 2X") { game.isOnBattlefield("Hill Giant") shouldBe true }
            withClue("the flyer is spared") { game.isOnBattlefield("Storm Crow") shouldBe true }
        }

        test("overload with X=2 can't be paid with only five lands — X counts twice in the cost") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Street Spasm")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castOverloaded(xValue = 2).error shouldNotBe null
        }
    }
}
