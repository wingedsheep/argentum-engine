package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.GloriousEnd
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "End the turn" ([com.wingedsheep.sdk.scripting.effects.EndTheTurnEffect] — Time Stop,
 * Discontinuity, Hurkyl's Final Meditation, Ultima, Glorious End) exiles every spell and ability on
 * the stack: as a card's whole payoff, a counter-everything read as [IntentTag.COUNTERSPELL] — dead
 * on an empty stack, except as a fog while the opponent's creatures attack. Beside another payoff
 * (Hurkyl's Final Meditation's bounce, Ultima's wrath) it is a rider, and the card reads as that
 * payoff. Glorious End also loses its caster the game, so it has no window at all.
 */
class EndTheTurnReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.choice(): GameAction = AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE).chooseAction(state)

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    init {
        test("ending the turn reads as a counterspell") {
            for (name in listOf("Glorious End", "Time Stop", "Discontinuity")) {
                withClue(name) { intents.forName(name)!!.tags shouldContain IntentTag.COUNTERSPELL }
            }
            val reading = AiReadingAudit.read(GloriousEnd)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("ending the turn beside another payoff reads as that payoff") {
            for (name in listOf("Hurkyl's Final Meditation", "Ultima")) {
                withClue(name) { intents.forName(name)!!.tags shouldNotContain IntentTag.COUNTERSPELL }
            }
        }

        test("does not cast Time Stop in its own main phase with nothing on the stack") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInHand(1, "Time Stop")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            withClue("chose ${game.choice()}") { game.castName(game.choice()) shouldNotBe "Time Stop" }
        }

        test("still casts Hurkyl's Final Meditation on its own turn to bounce the opponent's board") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 7)
                .withCardInHand(1, "Hurkyl's Final Meditation")
                .withCardOnBattlefield(2, "Serra Angel")
                .withCardOnBattlefield(2, "Serra Angel")
                .withCardOnBattlefield(2, "Craw Wurm")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val choice = game.choice()
            withClue("chose $choice") { game.castName(choice) shouldBe "Hurkyl's Final Meditation" }
        }

        test("Time Stop has a window as a fog while the opponent's creatures attack") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInHand(1, "Time Stop")
                .withCardOnBattlefield(2, "Craw Wurm")
                .build()
                .advanceToPriority(2, Step.BEGIN_COMBAT)
            game.passPriority()
            game.passPriority()
            game.declareAttackers(mapOf("Craw Wurm" to 1)).error shouldBe null
            game.passPriority()
            withClue("step ${game.state.step}, priority ${game.state.priorityPlayerId}") {
                game.state.step shouldBe Step.DECLARE_ATTACKERS
                game.state.priorityPlayerId shouldBe game.player1Id
            }
            val timeStop = game.state.getHand(game.player1Id).single()
            HoldPolicy(intents).verdictFor(
                game.state, game.player1Id, "Time Stop", cast = CastSpell(game.player1Id, timeStop),
            ) shouldNotBe TimingVerdict.NoWindow
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
