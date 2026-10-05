package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Tezzeret, Artifice Master (M19 #79, {3}{U}{U} planeswalker, loyalty 5).
 *
 *   +1: Create a 1/1 colorless Thopter artifact creature token with flying.
 *   0: Draw a card. If you control three or more artifacts, draw two cards instead.
 *   -9: You get an emblem with "At the beginning of your end step, search your library for a
 *       permanent card, put it onto the battlefield, then shuffle."
 */
class TezzeretArtificeMasterScenarioTest : ScenarioTestBase() {

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun ability(index: Int) =
        cardRegistry.getCard("Tezzeret, Artifice Master")!!.script.activatedAbilities[index]

    private fun activate(game: TestGame, tezz: EntityId, index: Int) {
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = tezz, abilityId = ability(index).id)
        ).error shouldBe null
        game.resolveStack()
    }

    init {
        test("+1 creates a 1/1 flying Thopter artifact token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tezzeret, Artifice Master")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tezz = game.findPermanent("Tezzeret, Artifice Master")!!
            seedLoyalty(game, tezz, 5)

            activate(game, tezz, 0)

            loyalty(game, tezz) shouldBe 6
            val thopter = game.findPermanent("Thopter Token") ?: game.findPermanent("Thopter")
            withClue("a Thopter token was created") { thopter shouldNotBe null }
            val projected = game.state.projectedState
            projected.isCreature(thopter!!) shouldBe true
            projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
            projected.getPower(thopter) shouldBe 1
            projected.getToughness(thopter) shouldBe 1
            withClue("the Thopter is a colorless artifact, so it counts for the 0 ability") {
                projected.hasType(thopter, "ARTIFACT") shouldBe true
                projected.getColors(thopter) shouldBe emptySet()
            }
        }

        test("0 draws one card with fewer than three artifacts") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tezzeret, Artifice Master")
                .withCardOnBattlefield(1, "Millstone")
                .withCardOnBattlefield(1, "Millstone")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tezz = game.findPermanent("Tezzeret, Artifice Master")!!
            seedLoyalty(game, tezz, 5)
            val handBefore = game.state.getHand(game.player1Id).size

            activate(game, tezz, 1)

            loyalty(game, tezz) shouldBe 5
            game.state.getHand(game.player1Id).size shouldBe handBefore + 1
        }

        test("0 draws two cards instead with three or more artifacts") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tezzeret, Artifice Master")
                .withCardOnBattlefield(1, "Millstone")
                .withCardOnBattlefield(1, "Millstone")
                .withCardOnBattlefield(1, "Millstone")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tezz = game.findPermanent("Tezzeret, Artifice Master")!!
            seedLoyalty(game, tezz, 5)
            val handBefore = game.state.getHand(game.player1Id).size

            activate(game, tezz, 1)

            game.state.getHand(game.player1Id).size shouldBe handBefore + 2
        }

        test("-9 emblem puts a permanent card from library onto the battlefield at your end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tezzeret, Artifice Master")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Shock")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tezz = game.findPermanent("Tezzeret, Artifice Master")!!
            seedLoyalty(game, tezz, 9)

            activate(game, tezz, 2)
            withClue("Tezzeret went to 0 loyalty and died") {
                game.findPermanent("Tezzeret, Artifice Master") shouldBe null
            }

            val bears = game.state.getLibrary(game.player1Id).single { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val shock = game.state.getLibrary(game.player1Id).single { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Shock"
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the emblem's end-step trigger asks for a permanent card") {
                game.hasPendingDecision() shouldBe true
            }
            val decision = game.state.pendingDecision as SelectCardsDecision
            withClue("an instant is not a permanent card") { decision.options.contains(shock) shouldBe false }
            decision.options.contains(bears) shouldBe true
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears was put onto the battlefield") {
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }
        }
    }
}
