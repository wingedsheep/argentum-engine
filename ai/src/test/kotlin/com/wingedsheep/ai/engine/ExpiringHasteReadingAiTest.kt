package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Haste granted until end of turn buys one thing — a creature that came in this turn attacking now —
 * and the cast's own simulation in our precombat main runs that attack, so it needs no hold of its
 * own. A colour change until end of turn ([com.wingedsheep.sdk.scripting.effects.ChangeColorEffect])
 * fights nothing of its own: what it changes is on the board the moment it resolves. The audit
 * reads both as timed.
 *
 * The decision that proves the reading: Crimson Wisps is cast before combat when its haste sends a
 * summoning-sick Grizzly Bears into an empty board, and held when the Bears attack without it.
 */
class ExpiringHasteReadingAiTest : ScenarioTestBase() {

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    private fun TestGame.choice(): GameAction =
        AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE).chooseAction(state)

    private fun position(sick: Boolean) = scenario().withPlayers()
        .withTurnNumber(5)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = sick)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInHand(1, "Crimson Wisps")
        .apply { repeat(5) { withCardInLibrary(1, "Mountain") } }
        .apply { repeat(5) { withCardInLibrary(2, "Mountain") } }
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("Crimson Wisps reads fully") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard("Crimson Wisps"))
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }

        test("haste that sends a fresh creature into the attack is cast before combat") {
            val game = position(sick = true)
            withClue("haste lets the summoning-sick Bears attack an empty board") {
                game.castName(game.choice()) shouldBe "Crimson Wisps"
            }
        }

        test("haste on a creature that can attack anyway buys no attack and waits") {
            val game = position(sick = false)
            withClue("the Bears attack with or without it, so the cantrip keeps its end-step window") {
                game.castName(game.choice()) shouldBe null
            }
        }
    }
}
