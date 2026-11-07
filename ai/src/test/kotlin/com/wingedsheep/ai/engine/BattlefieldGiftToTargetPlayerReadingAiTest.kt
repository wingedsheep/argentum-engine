package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A pipeline that puts cards onto the battlefield under a *target player's* control —
 * "Target opponent may choose one of the exiled cards and put it onto the battlefield under their
 * control" (Dubious Challenge) — hands that player a body, as a token made for them does. A slot
 * only an opponent can fill is the spell's price, not a side to pick, so it reads
 * [TargetPolarity.FLEXIBLE]: understood, but pruning nothing.
 *
 * Unread, the slot was an unknown in the reading audit; read as a gift it would veto the cast,
 * since every legal target is an opponent.
 */
class BattlefieldGiftToTargetPlayerReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a card put onto the battlefield under a target opponent's control reads as the spell's price") {
            withClue("Dubious Challenge: the opponent's pick is a body on their side, the cost of ours") {
                intents.spellTargetPolarities("Dubious Challenge") shouldBe listOf(TargetPolarity.FLEXIBLE)
            }
        }

        test("Dubious Challenge is not vetoed for aiming at an opponent") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInHand(1, "Dubious Challenge")
                .withCardInLibrary(1, "Craw Wurm")
                .withCardInLibrary(1, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
                val spell = action.action as? CastSpell ?: return@first false
                game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Dubious Challenge"
            }

            withClue("the only legal target is an opponent, and giving them one card is what the spell costs") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, intents) shouldBe false
            }
        }
    }
}
