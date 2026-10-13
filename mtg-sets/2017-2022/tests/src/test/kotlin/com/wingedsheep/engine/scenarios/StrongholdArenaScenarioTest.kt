package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Stronghold Arena (DMU #110).
 *
 * "Kicker {G} and/or {W}. When this enchantment enters, you gain 3 life for each time it was kicked.
 *  Whenever one or more creatures you control deal combat damage to a player, you may reveal the
 *  top card of your library and put it into your hand. If you do, you lose life equal to its mana
 *  value."
 *
 * "Each time it was kicked" sums the two linked kicker slots (0, 1 or 2) on the permanent the spell
 * became; the combat payoff charges the taken card's mana value.
 */
class StrongholdArenaScenarioTest : ScenarioTestBase() {

    private fun kickerBoard(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Stronghold Arena")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Plains", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castAndResolve(variant: String?) {
        val cardId = findCardsInHand(1, "Stronghold Arena").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = if (variant == null) {
            actions.single { it.actionType == "CastSpell" }
        } else {
            actions.single { it.actionType == "CastWithKicker" && it.description.endsWith("($variant)") }
        }
        execute(chosen.action).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) passPriority().error shouldBe null
        state.stack.isEmpty() shouldBe true
        isOnBattlefield("Stronghold Arena") shouldBe true
    }

    init {
        test("unkicked: no life gained") {
            val game = kickerBoard()
            game.castAndResolve(null)
            game.getLifeTotal(1) shouldBe 20
        }

        test("kicked once: gain 3") {
            val game = kickerBoard()
            game.castAndResolve("Kicked {W}")
            game.getLifeTotal(1) shouldBe 23
        }

        test("kicked twice: gain 6") {
            val game = kickerBoard()
            game.castAndResolve("Kicked {G} + {W}")
            game.getLifeTotal(1) shouldBe 26
        }

        context("combat damage payoff") {
            fun combatBoard(): TestGame = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Stronghold Arena")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
                .withCardInLibrary(1, "Lava Axe")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.attackWithBoth() {
                advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                declareAttackers(mapOf("Grizzly Bears" to 2, "Centaur Courser" to 2)).error shouldBe null
                passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                declareBlockers(emptyMap()).error shouldBe null
                passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                resolveStack()
            }

            test("taking the card puts it in hand and costs its mana value; one trigger for two attackers") {
                val game = combatBoard()
                game.attackWithBoth()

                withClue("one batched trigger asks once") { game.hasPendingDecision() shouldBe true }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInHand(1, "Lava Axe") shouldBe true
                withClue("Lava Axe has mana value 5") { game.getLifeTotal(1) shouldBe 15 }
                game.getLifeTotal(2) shouldBe 15
            }

            test("declining leaves the card on top and costs no life") {
                val game = combatBoard()
                game.attackWithBoth()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Lava Axe") shouldBe false
                game.librarySize(1) shouldBe 1
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
