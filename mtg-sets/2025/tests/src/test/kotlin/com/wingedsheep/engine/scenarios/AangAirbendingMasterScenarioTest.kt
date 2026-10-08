package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Aang, Airbending Master — airbend ETB, "one or more creatures you control leave the battlefield
 * without dying → you get an experience counter", and "at the beginning of your upkeep, create a
 * 1/1 white Ally for each experience counter you have".
 *
 * Pins that experience counters live on the player (CR 122.1) and reach the client, that a batch
 * of creatures leaving together is one trigger (one counter), and that the upkeep payoff counts
 * the player's total.
 */
class AangAirbendingMasterScenarioTest : ScenarioTestBase() {

    private fun experience(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.EXPERIENCE) ?: 0

    init {
        test("ETB airbends another target creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Aang, Airbending Master")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Aang, Airbending Master").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("the opponent's creature is airbent (exiled)") {
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
            withClue("an exiled opponent's creature doesn't count — it isn't a creature you control") {
                experience(game, game.player1Id) shouldBe 0
            }
        }

        test("creatures leaving together without dying give one experience counter, and upkeep makes that many Allies") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Aang, Airbending Master")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardInHand(1, "Eerie Interlude")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val seeker = game.findPermanent("Glory Seeker")!!
            val action = game.getLegalActions(1).first {
                it.action is CastSpell && game.state.getEntity((it.action as CastSpell).cardId)
                    ?.get<CardComponent>()?.name == "Eerie Interlude"
            }
            val cast = (action.action as CastSpell).copy(
                targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(seeker))
            )
            game.execute(cast).error shouldBe null
            game.resolveStack()

            withClue("two creatures exiled at once is one 'one or more' trigger — one counter") {
                experience(game, game.player1Id) shouldBe 1
            }
            withClue("the counter reaches the client as a player badge") {
                game.getClientState(1).players.first { it.playerId == game.player1Id }.experienceCounters shouldBe 1
            }

            // To P1's next upkeep (through P2's turn, where "your upkeep" doesn't trigger).
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            var guard = 0
            while (!(game.state.activePlayerId == game.player1Id && game.state.step == Step.UPKEEP) && guard++ < 4) {
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                if (game.state.activePlayerId != game.player1Id) game.passUntilPhase(Phase.ENDING, Step.END)
            }
            game.resolveStack()

            withClue("one 1/1 white Ally token per experience counter") {
                val tokens = game.state.getBattlefield().filter {
                    game.state.getEntity(it)?.has<TokenComponent>() == true
                }
                tokens.size shouldBe 1
                val token = tokens.single()
                game.state.projectedState.getPower(token) shouldBe 1
                game.state.projectedState.getToughness(token) shouldBe 1
            }
            withClue("the counter stays on the player") {
                experience(game, game.player1Id) shouldBe 1
            }
        }
    }
}
