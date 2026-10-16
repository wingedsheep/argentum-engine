package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mariposa Military Base (PIP #151): you may have it enter tapped, and only if you do you get two
 * rad counters; its draw ability costs {1} less for each rad counter you have.
 */
class MariposaMilitaryBaseScenarioTest : ScenarioTestBase() {

    private fun radCount(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0

    private fun seedRad(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.RAD, amount))
        }
    }

    private fun drawAbilityId() = cardRegistry.getCard("Mariposa Military Base")!!
        .activatedAbilities.first { !it.isManaAbility }.id

    private fun playBase(): TestGame {
        val game = scenario().withPlayers("Player1", "Player2")
            .withCardInHand(1, "Mariposa Military Base")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val base = game.findCardsInHand(1, "Mariposa Military Base").first()
        game.execute(PlayLand(game.player1Id, base)).error shouldBe null
        withClue("the optional tapped entry asks the player") {
            game.hasPendingDecision() shouldBe true
        }
        return game
    }

    init {
        test("entering tapped gives you two rad counters") {
            val game = playBase()
            game.answerYesNo(true).error shouldBe null

            val base = game.findPermanent("Mariposa Military Base")!!
            game.state.getEntity(base)!!.has<TappedComponent>() shouldBe true
            radCount(game, game.player1Id) shouldBe 2
            radCount(game, game.player2Id) shouldBe 0
        }

        test("declining leaves it untapped and gives no rad counters") {
            val game = playBase()
            game.answerYesNo(false).error shouldBe null

            val base = game.findPermanent("Mariposa Military Base")!!
            game.state.getEntity(base)!!.has<TappedComponent>() shouldBe false
            radCount(game, game.player1Id) shouldBe 0
        }

        test("each rad counter you have makes the draw ability cost {1} less") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mariposa Military Base")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            seedRad(game, game.player1Id, 3)
            val base = game.findPermanent("Mariposa Military Base")!!
            val handBefore = game.handSize(1)

            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = base, abilityId = drawAbilityId())
            )
            withClue("{5} less three rad counters is {2}, paid by two Mountains: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()
            game.handSize(1) shouldBe handBefore + 1
        }

        test("without enough rad counters the full {5} can't be paid") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mariposa Military Base")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            seedRad(game, game.player1Id, 2)
            val base = game.findPermanent("Mariposa Military Base")!!

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = base, abilityId = drawAbilityId())
            ).error shouldNotBe null
        }
    }
}
