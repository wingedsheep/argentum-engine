package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tribute to Horobi // Echo of Death's Wail (NEO #124).
 *
 * Chapters I and II hand each opponent a 1/1 Rat Rogue token; chapter III flips the Saga into
 * Echo of Death's Wail, whose enter trigger takes back every Rat *token* — the opponent's nontoken
 * Rat stays where it is.
 */
class TributeToHorobiScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.toMyNextMain() {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            resolveStack()
        }

        fun TestGame.ratTokens(): List<EntityId> = state.getBattlefield().filter { id ->
            state.getEntity(id)?.has<TokenComponent>() == true &&
                state.projectedState.hasSubtype(id, "Rat")
        }

        context("Tribute to Horobi") {

            test("chapters I and II give the opponent Rat Rogues; Echo of Death's Wail steals every Rat token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tribute to Horobi")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Nezumi Graverobber")
                    .apply { repeat(6) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Swamp") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Tribute to Horobi").error shouldBe null
                game.resolveStack()

                withClue("chapter I: the opponent creates one 1/1 black Rat Rogue") {
                    val rats = game.ratTokens()
                    rats.size shouldBe 1
                    val rat = rats.single()
                    game.state.projectedState.getController(rat) shouldBe game.player2Id
                    game.state.projectedState.getPower(rat) shouldBe 1
                    game.state.projectedState.getToughness(rat) shouldBe 1
                    game.state.projectedState.hasSubtype(rat, "Rogue") shouldBe true
                }

                game.toMyNextMain()
                withClue("chapter II: a second Rat for the opponent") {
                    game.ratTokens().map { game.state.projectedState.getController(it) } shouldBe
                        listOf(game.player2Id, game.player2Id)
                }

                game.toMyNextMain()
                withClue("chapter III returned the Saga transformed") {
                    game.findPermanent("Tribute to Horobi") shouldBe null
                    game.isOnBattlefield("Echo of Death's Wail") shouldBe true
                }
                withClue("both Rat tokens are now ours") {
                    game.ratTokens().map { game.state.projectedState.getController(it) } shouldBe
                        listOf(game.player1Id, game.player1Id)
                }
                withClue("a nontoken Rat isn't a Rat token") {
                    val graverobber = game.findPermanent("Nezumi Graverobber")!!
                    game.state.projectedState.getController(graverobber) shouldBe game.player2Id
                }
            }
        }
    }
}
