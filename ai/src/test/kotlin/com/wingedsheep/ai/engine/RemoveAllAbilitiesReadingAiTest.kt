package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Target creature loses all abilities" ([com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect])
 * strips the creature it lands on: the slot hurts its holder, as the static `LoseAllAbilities` an
 * Aura grants already reads. Unread, the slot was two-sided and Lizard, Connors's Curse's enter
 * trigger could strip our own Serra Angel instead of the opponent's.
 */
class RemoveAllAbilitiesReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.permanent(playerId: EntityId, name: String): EntityId =
        state.getBattlefield(playerId).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("losing all abilities hurts the creature that loses them") {
            for (card in listOf("Lizard, Connors's Curse", "Azure Beastbinder")) {
                val trigger = cardRegistry.requireCard(card).script.triggeredAbilities.first { it.targetRequirement != null }
                withClue("$card: 'loses all abilities' is aimed at a creature we want weaker") {
                    intents.effectTargetPolarities(trigger.effect, listOf(trigger.targetRequirement!!), card) shouldBe
                        listOf(TargetPolarity.HARMFUL)
                }
            }
        }

        test("Lizard, Connors's Curse strips the opponent's Serra Angel, not ours") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInHand(1, "Lizard, Connors's Curse")
                .withCardOnBattlefield(1, "Serra Angel")
                .withCardOnBattlefield(2, "Serra Angel")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Lizard, Connors's Curse").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val ai = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
            val response = ai.respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            withClue("losing flying and vigilance is a loss: it goes on the opponent's angel") {
                response.selectedTargets.values.flatten() shouldBe listOf(game.permanent(game.player2Id, "Serra Angel"))
            }
        }
    }
}
