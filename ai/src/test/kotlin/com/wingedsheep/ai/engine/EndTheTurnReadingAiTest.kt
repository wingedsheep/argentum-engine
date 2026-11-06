package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.GloriousEnd
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "End the turn" ([com.wingedsheep.sdk.scripting.effects.EndTheTurnEffect] — Time Stop,
 * Discontinuity, Hurkyl's Final Meditation, Ultima, Glorious End) exiles every spell and ability on
 * the stack: a counter-everything, read as [IntentTag.COUNTERSPELL]. Like any counterspell it is
 * dead on an empty stack — so the AI does not end its own main phase with Glorious End, which would
 * buy nothing and cost it the game at its next end step. Nor does it cast Glorious End to answer a
 * spell: a spell whose resolution loses its controller the game has no window at all.
 */
class EndTheTurnReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("ending the turn reads as a counterspell") {
            for (name in listOf("Glorious End", "Time Stop", "Discontinuity")) {
                withClue(name) { intents.forName(name)!!.tags shouldContain IntentTag.COUNTERSPELL }
            }
            val reading = AiReadingAudit.read(GloriousEnd)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("does not cast Glorious End in its own main phase with nothing on the stack") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(1, "Glorious End")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val choice = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            val cast = (choice as? CastSpell)?.let { game.state.getEntity(it.cardId)?.get<CardComponent>()?.name }
            withClue("chose $choice") { (cast == "Glorious End") shouldBe false }
        }

        // The countered spell is what the 1-ply simulation scores; the loss at our next end step it
        // never sees. Exiling a Serra Angel for the game is no trade.
        test("does not cast Glorious End to exile an opponent's spell, which would lose it the game") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(1, "Glorious End")
                .withLandsOnBattlefield(2, "Plains", 5)
                .withCardInHand(2, "Serra Angel")
                .build()
                .advanceToPriority(2, Step.PRECOMBAT_MAIN)
            game.castSpell(2, "Serra Angel").error shouldBe null
            game.passPriority()
            withClue("Serra Angel is on the stack") { game.state.stack.size shouldBe 1 }
            val choice = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            withClue("chose $choice") { (choice is CastSpell) shouldBe false }
        }
    }
}
