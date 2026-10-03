package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sundering Eruption // Volcanic Fissure (MH3).
 *
 * Front: "Destroy target land. Its controller may search their library for a basic land card, put it
 * onto the battlefield tapped, then shuffle. Creatures without flying can't block this turn."
 * Back: "As this land enters, you may pay 3 life. If you don't, it enters tapped. {T}: Add {R}."
 */
class SunderingEruptionScenarioTest : ScenarioTestBase() {

    private fun eruptionGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Sundering Eruption")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Island")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Wind Drake")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun landsNamed(game: TestGame, name: String, controller: EntityId): List<EntityId> =
        game.state.getBattlefield().filter { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name &&
                game.state.projectedState.getController(id) == controller
        }

    private fun castAt(game: TestGame, land: EntityId) {
        val card = game.state.getHand(game.player1Id).single()
        game.execute(
            CastSpell(playerId = game.player1Id, cardId = card, targets = listOf(ChosenTarget.Permanent(land)))
        ).error shouldBe null
    }

    private fun resolveAnswering(game: TestGame, search: Boolean) {
        game.resolveStack()
        var safety = 0
        while (safety++ < 20) {
            when (val pending = game.state.pendingDecision) {
                is YesNoDecision -> {
                    pending.playerId shouldBe game.player2Id
                    game.answerYesNo(search).error shouldBe null
                }
                is SelectCardsDecision -> {
                    pending.playerId shouldBe game.player2Id
                    game.selectCards(pending.options.take(1)).error shouldBe null
                }
                null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else break
                else -> error("Unexpected decision $pending")
            }
        }
    }

    init {
        context("Sundering Eruption") {

            test("destroys the land; its controller fetches a tapped basic") {
                val game = eruptionGame()
                val island = game.findPermanent("Island")!!
                castAt(game, island)
                resolveAnswering(game, search = true)

                game.isInGraveyard(2, "Island") shouldBe true
                game.isInGraveyard(1, "Sundering Eruption") shouldBe true
                val forests = landsNamed(game, "Forest", game.player2Id)
                withClue("the opponent — the destroyed land's controller — fetched one Forest, tapped") {
                    forests.size shouldBe 1
                    game.state.getEntity(forests.single())!!.has<TappedComponent>() shouldBe true
                }
                landsNamed(game, "Mountain", game.player1Id).size shouldBe 3
            }

            test("the controller may decline the search") {
                val game = eruptionGame()
                castAt(game, game.findPermanent("Island")!!)
                resolveAnswering(game, search = false)

                game.isInGraveyard(2, "Island") shouldBe true
                landsNamed(game, "Forest", game.player2Id).size shouldBe 0
            }

            test("creatures without flying can't block this turn; fliers still can") {
                val game = eruptionGame()
                castAt(game, game.findPermanent("Island")!!)
                resolveAnswering(game, search = false)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Grizzly Bears has no flying and can't block") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
                }
                withClue("Wind Drake has flying and can still block") {
                    game.declareBlockers(mapOf("Wind Drake" to listOf("Hill Giant"))).error shouldBe null
                }
            }
        }

        context("Volcanic Fissure — the land back") {

            test("paying 3 life has it enter untapped") {
                val game = eruptionGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Volcanic Fissure")!!
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = eruptionGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Volcanic Fissure")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
