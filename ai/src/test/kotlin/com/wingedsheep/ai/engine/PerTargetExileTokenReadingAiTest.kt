package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * A pipeline that gathers each chosen target on its own and exiles it behind an "if you do" gate —
 * Hour of Need's "exile any number of target creatures. For each creature exiled this way, its
 * controller creates a 4/4 blue Sphinx", spelled per target as
 * `IfYouDo(moveTracked(gather(CardSource.Entity(t)), ToZone(EXILE)), then = CreateToken(TargetController))`.
 * The gate's action is the card's removal, not a price, so the slot reads HARMFUL and the AI aims it
 * at the opponent's creatures rather than its own.
 */
class PerTargetExileTokenReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun hourCast(game: TestGame) =
        GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
            val spell = action.action as? CastSpell ?: return@first false
            game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == HOUR
        }

    private fun board(ours: List<String>, theirs: List<String>) = scenario()
        .withPlayers()
        .withTurnNumber(6)
        .withActivePlayer(1)
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardInHand(1, HOUR)
        .apply { ours.forEach { withCardOnBattlefield(1, it) } }
        .apply { theirs.forEach { withCardOnBattlefield(2, it) } }
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("Hour of Need reads its target as exile removal") {
            val card = cardRegistry.getCard(HOUR)!!
            TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.HARMFUL)
            val reading = AiReadingAudit.read(card)
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }

        test("Hour of Need's intent is exile removal, beside the token its victim's controller gets") {
            val tags = CardIntentAnalyzer.analyze(cardRegistry.getCard(HOUR)!!).tags
            tags shouldContainAll listOf(IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL)
        }

        test("Hour of Need ranks the opponent's creature ahead of ours") {
            val game = board(ours = listOf("Grizzly Bears"), theirs = listOf("Grizzly Bears"))
            val action = hourCast(game)
            val rank = TargetSelection.ranker(game.state, action, game.player1Id, intents, polarityFromEffect = true)
            val best = TargetSelection.targetInfosFor(action)!!.single().validTargets.maxBy(rank)
            TargetSelection.isOurs(game.state, best, game.player1Id) shouldBe false
            TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, intents) shouldBe false
        }
    }

    private companion object {
        const val HOUR = "Hour of Need"
    }
}
