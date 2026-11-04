package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A **triggered** "return target creature to its owner's hand" (Deadeye Rig-Hauler's raid, Man-o'-War,
 * Aethersnipe) reads as harmful: a trigger fires on its event's schedule, so it never offers the
 * instant-speed "save my own creature from removal" play that keeps an Unsummon's slot two-sided.
 * A bounce whose filter keeps it on our own side is the rebuy shape and stays unread.
 *
 * Unread, the slot fell back to the plain removal ranking, which scores every noncreature permanent
 * 0 whoever controls it — so Aethersnipe facing only lands and an enchantment took whichever came
 * first, our own included.
 */
class TriggeredBounceReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun triggerPolarities(card: String, index: Int = 0): List<TargetPolarity> {
        val trigger = cardRegistry.getCard(card)!!.script.triggeredAbilities[index]
        val reqs = listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements
        return intents.effectTargetPolarities(trigger.effect, reqs, card)
    }

    init {
        test("a triggered bounce reads as harmful; an instant's stays two-sided") {
            withClue("Deadeye Rig-Hauler: raid — you may return target creature to its owner's hand") {
                triggerPolarities("Deadeye Rig-Hauler") shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("Aethersnipe: return target nonartifact permanent to its owner's hand") {
                triggerPolarities("Aethersnipe") shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("Unsummon: at instant speed the bounce can save our own creature") {
                intents.spellTargetPolarities("Unsummon") shouldBe listOf(TargetPolarity.UNKNOWN)
            }
        }

        test("Aethersnipe bounces the opponent's permanent, not one of ours") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInHand(1, "Aethersnipe")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardOnBattlefield(2, "Glorious Anthem")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Aethersnipe").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            val picked = response.selectedTargets.values.flatten().single()
            withClue("the bounce is tempo removal: it goes to the opponent's side") {
                game.state.getEntity(picked)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
            }
        }
    }
}
