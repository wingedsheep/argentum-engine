package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Exchange life totals with target player" (Psychic Transfer, Mister Negative, Mirror Universe)
 * and "two target players exchange life totals" (Axis of Mortality).
 *
 * The exchange only ever pays its controller off when they are one of the two players: with the
 * controller as the fixed side, the target slot is the opponent to swap with (swapping with
 * yourself does nothing); with both sides chosen, the controller wants to be one of them. Unread,
 * the slot was a coin toss, and the AI could aim Psychic Transfer at itself — a sorcery cast for
 * nothing.
 */
class ExchangeLifeTotalsReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("an exchange with the controller reads harmful to its target; a two-target exchange wants the controller in it") {
            withClue("Psychic Transfer: exchanging with target player is aimed at an opponent") {
                intents.spellTargetPolarities("Psychic Transfer") shouldBe listOf(TargetPolarity.HARMFUL)
            }
            val axis = cardRegistry.getCard("Axis of Mortality")!!.script.triggeredAbilities.single()
            withClue("Axis of Mortality: two target players exchange — the controller picks itself as one of them") {
                intents.effectTargetPolarities(axis.effect, listOfNotNull(axis.targetRequirement) + axis.additionalTargetRequirements) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("an exchange of life totals reads as life gain for its controller") {
            listOf("Psychic Transfer", "Mister Negative", "Axis of Mortality").forEach { name ->
                withClue("$name: the controller's side of an exchange is a life gain") {
                    CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!).tags shouldContain IntentTag.LIFEGAIN
                }
            }
        }

        test("Psychic Transfer is not cast at ourselves when the opponent can't be targeted") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLifeTotal(1, 4)
                .withLifeTotal(2, 9)
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInHand(1, "Psychic Transfer")
                .withCardOnBattlefield(2, "Leyline of Sanctity")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
                val spell = action.action as? CastSpell ?: return@first false
                game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Psychic Transfer"
            }

            withClue("with the opponent hexproof the only target is ourselves, and exchanging with ourselves does nothing") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, intents) shouldBe true
            }
        }
    }
}
