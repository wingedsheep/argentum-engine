package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Borborygmos Enraged (GTC #147) — combat damage to a player reveals the top three cards, lands go
 * to hand and the rest to the graveyard; "Discard a land card: 3 damage to any target."
 */
class BorborygmosEnragedScenarioTest : ScenarioTestBase() {
    init {
        test("combat damage to a player puts revealed lands into hand and the rest into the graveyard") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Borborygmos Enraged", summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Borborygmos Enraged" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            if (game.state.pendingDecision != null) game.submitDefaultCombatDamage()
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20 - 7
            withClue("both revealed lands went to hand") {
                game.isInHand(1, "Forest") shouldBe true
                game.isInHand(1, "Mountain") shouldBe true
            }
            withClue("the nonland went to the graveyard") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            game.librarySize(1) shouldBe 0
        }

        test("discarding a land card deals 3 damage to any target") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Borborygmos Enraged")
                .withCardInHand(1, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val borborygmos = game.findPermanent("Borborygmos Enraged")!!
            val abilityId = cardRegistry.getCard("Borborygmos Enraged")!!.script.activatedAbilities.single().id
            val forest = game.findCardsInHand(1, "Forest").single()

            val result = game.execute(
                ActivateAbility(
                    game.player1Id,
                    borborygmos,
                    abilityId,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(discardedCards = listOf(forest)),
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.isInGraveyard(1, "Forest") shouldBe true
        }
    }
}
