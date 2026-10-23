package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Augmenter Pugilist // Echoing Equation (STX #147) — a modal double-faced card.
 *
 * Back — Echoing Equation ({3}{U}{U} Sorcery): "Choose target creature you control. Each other
 * creature you control becomes a copy of it until end of turn, except those creatures aren't
 * legendary."
 *
 * The first targeted spell back of a modal DFC: the face's own target requirement is read when
 * it is cast via `faceIndex = 0`, the target keeps its identity, every other creature you control
 * becomes a nonlegendary copy (so the legend rule leaves them all alive), and the copies revert
 * at end of turn.
 */
class AugmenterPugilistScenarioTest : ScenarioTestBase() {

    private val legendaryHero = card("Legendary Test Hero") {
        manaCost = "{2}{W}"
        typeLine = "Legendary Creature — Human Soldier"
        power = 4
        toughness = 4
    }

    private fun cardOf(game: TestGame, id: EntityId): CardComponent? =
        game.state.getEntity(id)?.get<CardComponent>()

    init {
        cardRegistry.register(legendaryHero)

        context("Echoing Equation") {
            test("each other creature you control becomes a nonlegendary copy of the target until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Augmenter Pugilist")
                    .withCardOnBattlefield(1, "Legendary Test Hero")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Augmenter Pugilist").first()
                val hero = game.findPermanent("Legendary Test Hero")!!
                val bears = game.state.getBattlefield().first { id ->
                    cardOf(game, id)?.name == "Grizzly Bears" &&
                        game.state.projectedState.getController(id) == game.player1Id
                }
                val opponentBears = game.state.getBattlefield().first { id ->
                    cardOf(game, id)?.name == "Grizzly Bears" &&
                        game.state.projectedState.getController(id) == game.player2Id
                }
                val giant = game.findPermanent("Hill Giant")!!

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        targets = listOf(ChosenTarget.Permanent(hero)),
                        faceIndex = 0,
                    )
                )
                withClue("Casting Echoing Equation should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The target keeps its own identity and stays legendary") {
                    cardOf(game, hero)?.name shouldBe "Legendary Test Hero"
                    game.state.projectedState.isLegendary(hero) shouldBe true
                }
                for (copy in listOf(bears, giant)) {
                    withClue("Each other creature you control becomes a 4/4 nonlegendary copy") {
                        game.state.getBattlefield().contains(copy) shouldBe true
                        cardOf(game, copy)?.name shouldBe "Legendary Test Hero"
                        game.state.projectedState.isLegendary(copy) shouldBe false
                        game.state.projectedState.getPower(copy) shouldBe 4
                        game.state.projectedState.getToughness(copy) shouldBe 4
                    }
                }
                withClue("The original survives the legend rule alongside its copies") {
                    game.state.getBattlefield().contains(hero) shouldBe true
                }
                withClue("An opponent's creature is unaffected") {
                    cardOf(game, opponentBears)?.name shouldBe "Grizzly Bears"
                }
                withClue("The modal DFC goes to the graveyard after resolving") {
                    game.state.getGraveyard(game.player1Id).contains(cardId) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("After end of turn the copies revert") {
                    cardOf(game, bears)?.name shouldBe "Grizzly Bears"
                    cardOf(game, giant)?.name shouldBe "Hill Giant"
                    game.state.projectedState.getPower(giant) shouldBe 3
                }
            }
        }
    }
}
