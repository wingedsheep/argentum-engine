package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A dynamic "-X/-X" on another creature ([com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect]
 * whose modifiers are a negated count or X): Death Wind, Defile, Bloodtithe Harvester's sacrifice.
 *
 * A fixed "-N/-N" was already removal; a dynamic one fell through the reader because only a
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed] toughness was read as a shrink. Unread,
 * the card had no intent, so every consumer that prices removal — keeping it over a filler body
 * when a card must go, the response and end-step windows — saw a do-nothing spell.
 */
class ModifyStatsReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a dynamic -X/-X on a target is removal") {
            for (name in listOf("Death Wind", "Defile", "Bloodtithe Harvester")) {
                withClue("$name: -X/-X on target creature reads as removal") {
                    intents.forName(name)!!.tags shouldContain IntentTag.REMOVAL
                }
            }
        }

        test("forced to discard, keeps Death Wind and pitches the vanilla four-drop") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(5)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Death Wind")
                .withCardInHand(1, "Hill Giant")
                .withCardOnBattlefield(2, "Craw Wurm")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val hand = game.state.getHand(game.player1Id).toList()
            val decision = SelectCardsDecision(
                id = "test",
                playerId = game.player1Id,
                prompt = "Choose a card to discard",
                context = DecisionContext(phase = DecisionPhase.RESOLUTION),
                options = hand,
                minSelections = 1,
                maxSelections = 1,
            )
            val ai = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
            val pitched = (ai.respondToDecision(game.state, decision) as CardsSelectedResponse)
                .selectedCards.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
            withClue("discarded $pitched — Death Wind reads as removal, worth more than a vanilla 3/3 facing a 6/4") {
                pitched shouldBe listOf("Hill Giant")
            }
        }
    }
}
