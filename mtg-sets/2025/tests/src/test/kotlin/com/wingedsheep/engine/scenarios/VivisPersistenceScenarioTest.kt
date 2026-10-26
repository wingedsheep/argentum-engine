package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vivi's Persistence (FIC #458) — "Whenever your commander enters or attacks, you may pay {2}. If you
 * do, return this card from your graveyard to your hand." Pins that the graveyard trigger fires for
 * your commander only — not for another attacker, nor an opponent's commander — and that entering
 * counts too. The spell half is the Kuja Wizard token, already covered by that card.
 */
class VivisPersistenceScenarioTest : ScenarioTestBase() {

    private fun TestGame.makeCommander(name: String, owner: Int) {
        val id = findPermanent(name) ?: state.getHand(if (owner == 1) player1Id else player2Id)
            .single { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == name }
        state = state.updateEntity(id) {
            it.with(CommanderComponent(ownerId = if (owner == 1) player1Id else player2Id))
        }
    }

    private fun board(activePlayer: Int = 1): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInGraveyard(1, "Vivi's Persistence")
            .withCardOnBattlefield(activePlayer, "Grizzly Bears")
            .withCardOnBattlefield(activePlayer, "Savannah Lions")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(activePlayer)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.makeCommander("Grizzly Bears", activePlayer)
        return game
    }

    init {
        test("your commander attacking: pay {2} to return it to hand") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
            withClue("one trigger — for the commander, not for the Lions") { game.state.stack.size shouldBe 1 }
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
            game.submitManaSourcesAutoPay().error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Vivi's Persistence") shouldBe true
            game.isInGraveyard(1, "Vivi's Persistence") shouldBe false
        }

        test("a non-commander attacking doesn't trigger it") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Savannah Lions" to 2)).error shouldBe null
            game.state.stack.size shouldBe 0
            game.hasPendingDecision() shouldBe false
        }

        test("an opponent's commander attacking doesn't trigger it") {
            val game = board(activePlayer = 2)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.state.stack.size shouldBe 0
            game.isInGraveyard(1, "Vivi's Persistence") shouldBe true
        }

        test("your commander entering triggers it; declining leaves the card in the graveyard") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInGraveyard(1, "Vivi's Persistence")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.makeCommander("Grizzly Bears", 1)
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Vivi's Persistence") shouldBe true
        }
    }
}
