package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.CurseOfExhaustion
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Curse of Exhaustion (DKA #5) — {2}{W}{W} Enchantment — Aura Curse.
 *
 * "Enchant player. Enchanted player can't cast more than one spell each turn."
 *
 * The per-turn cast cap binds the player the Curse is *attached to*, never its controller, and
 * spells that player cast earlier in the turn (before the Curse arrived) count toward it.
 */
class CurseOfExhaustionScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(CurseOfExhaustion))

        /** The Curse on player 1's battlefield, enchanting player 2. */
        fun cursed(activePlayer: Int): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Curse of Exhaustion")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Llanowar Elves")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(2, "Forest", 4)
                .withActivePlayer(activePlayer)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val curse = game.findPermanent("Curse of Exhaustion")!!
            game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(game.player2Id)) }
            return game
        }

        fun TestGame.canCast(playerNumber: Int, name: String): Boolean {
            val cardId = findCardsInHand(playerNumber, name).first()
            return getLegalActions(playerNumber).any { (it.action as? CastSpell)?.cardId == cardId }
        }

        fun TestGame.castAndResolve(playerNumber: Int, name: String) {
            castSpell(playerNumber, name).error shouldBe null
            if (hasPendingDecision()) submitManaSourcesAutoPay()
            resolveStack()
        }

        test("cast on an opponent, it enchants that player") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Curse of Exhaustion")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Curse of Exhaustion", 2).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val curse = game.findPermanent("Curse of Exhaustion")!!
            game.state.getEntity(curse)?.get<AttachedToComponent>()?.targetId shouldBe game.player2Id
        }

        test("the enchanted player casts one spell, then can't cast a second this turn") {
            val game = cursed(activePlayer = 2)
            withClue("the first spell of the turn is castable") { game.canCast(2, "Llanowar Elves") shouldBe true }
            game.castAndResolve(2, "Llanowar Elves")
            withClue("a second spell is not") { game.canCast(2, "Grizzly Bears") shouldBe false }
            withClue("and the engine refuses it outright") {
                (game.castSpell(2, "Grizzly Bears").error != null) shouldBe true
            }
        }

        test("a spell the enchanted player cast before the Curse arrived counts") {
            val game = cursed(activePlayer = 2)
            game.state = game.state.copy(playerSpellsCastThisTurn = mapOf(game.player2Id to 1))
            game.canCast(2, "Llanowar Elves") shouldBe false
        }

        test("the Curse's controller is not limited") {
            val game = cursed(activePlayer = 1)
            game.castAndResolve(1, "Llanowar Elves")
            withClue("its controller isn't the enchanted player") { game.canCast(1, "Grizzly Bears") shouldBe true }
        }
    }
}
