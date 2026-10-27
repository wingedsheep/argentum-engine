package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nahiri's Lithoforming — {X}{R}{R} Sorcery.
 *   "Sacrifice X lands. For each land sacrificed this way, draw a card. You may play X additional
 *    lands this turn. Lands you control enter tapped this turn."
 */
class NahirisLithoformingScenarioTest : ScenarioTestBase() {

    private fun TestGame.named(name: String, ids: List<EntityId>) =
        ids.filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.cast(x: Int) = execute(
        CastSpell(player1Id, named("Nahiri's Lithoforming", state.getHand(player1Id)).single(), emptyList(), xValue = x)
    )

    private fun TestGame.tapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    init {
        test("X = 2: sacrifice two lands, draw two, play three lands this turn — each enters tapped") {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nahiri's Lithoforming")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(4) { builder = builder.withCardInHand(1, "Forest") }
            repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
            val game = builder.build()

            game.cast(x = 2).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            val plains = game.findPermanent("Plains")!!
            val mountain = game.findAllPermanents("Mountain").first()
            game.selectCards(listOf(plains, mountain))

            game.findPermanent("Plains") shouldBe null
            game.findAllPermanents("Mountain") shouldHaveSize 3
            withClue("one card drawn per land sacrificed") {
                game.named("Island", game.state.getHand(game.player1Id)) shouldHaveSize 2
            }

            val forests = game.named("Forest", game.state.getHand(game.player1Id))
            forests.take(3).forEach { forest ->
                game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
                withClue("lands you control enter tapped this turn") { game.tapped(forest) shouldBe true }
            }
            withClue("the normal land play plus X = 2 additional") {
                game.execute(PlayLand(game.player1Id, forests[3])).error shouldNotBe null
            }
        }

        test("X greater than your lands: sacrifice them all unprompted, draw that many, still X more land plays") {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nahiri's Lithoforming")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(6) { builder = builder.withCardInHand(1, "Forest") }
            repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
            val game = builder.build()
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(ManaPoolComponent(colorless = 4))
            }

            withClue("{4}{R}{R}: two Mountains for {R}{R}, four colorless from the pool") {
                game.cast(x = 4).error shouldBe null
            }
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.findAllPermanents("Mountain") shouldHaveSize 0
            game.named("Island", game.state.getHand(game.player1Id)) shouldHaveSize 2

            val forests = game.named("Forest", game.state.getHand(game.player1Id))
            forests.take(5).forEach { game.execute(PlayLand(game.player1Id, it)).error shouldBe null }
            game.execute(PlayLand(game.player1Id, forests[5])).error shouldNotBe null
        }

        test("X = 0 sacrifices and draws nothing, but lands still enter tapped this turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nahiri's Lithoforming")
                .withCardInHand(1, "Forest")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast(x = 0).error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.findAllPermanents("Mountain") shouldHaveSize 2
            game.named("Island", game.state.getHand(game.player1Id)) shouldHaveSize 0

            val forests = game.named("Forest", game.state.getHand(game.player1Id))
            game.execute(PlayLand(game.player1Id, forests[0])).error shouldBe null
            game.tapped(forests[0]) shouldBe true
            game.execute(PlayLand(game.player1Id, forests[1])).error shouldNotBe null
        }
    }
}
