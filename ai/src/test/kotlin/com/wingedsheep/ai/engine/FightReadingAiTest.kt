package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
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
 * The target polarity of a fight ([com.wingedsheep.sdk.scripting.effects.FightEffect]).
 *
 * The creature fought is dealt damage: the victim's half reads harmful. The fighter, when its
 * filter keeps it on our side ("target creature you control fights …"), reads beneficial like a
 * bite's source: its power is the weapon, so our best creature is the one to fight with. A
 * fighter any player's creature can be stays a board question.
 *
 * Unread, the fighter fell back to the removal ranking, which orders our own creatures worst
 * first. A triggered ability's targets are scored with the trigger still on the stack, so the
 * simulation cannot separate them and that ranking decides: Yoshimaru's enter trigger sent a
 * Llanowar Elves into the opponent's Hill Giant while a Craw Wurm watched.
 */
class FightReadingAiTest : ScenarioTestBase() {

    private fun triggerSlots(name: String): List<TargetPolarity> {
        val trigger = cardRegistry.getCard(name)!!.script.triggeredAbilities.single()
        return TargetPolarityAnalyzer.forRequirements(trigger.effect, trigger.allTargetRequirements)
    }

    private fun TestGame.onBattlefield(playerId: EntityId, name: String): EntityId =
        state.getBattlefield(playerId).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("the creature fought reads as harmful") {
            withClue("Mind Meanderer: this creature fights up to one target creature an opponent controls") {
                triggerSlots("Mind Meanderer") shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("a fighter kept on our side reads as ours to lend") {
            withClue("Yoshimaru: another target creature you control fights up to one target creature") {
                triggerSlots("Yoshimaru, Scrappy Stray") shouldBe listOf(TargetPolarity.BENEFICIAL, TargetPolarity.HARMFUL)
            }
            withClue("Prey Upon: target creature you control fights target creature you don't control") {
                IntentCatalog.of(cardRegistry).spellTargetPolarities("Prey Upon") shouldBe
                    listOf(TargetPolarity.BENEFICIAL, TargetPolarity.HARMFUL)
            }
        }

        test("Yoshimaru's enter trigger fights with Craw Wurm, not Llanowar Elves") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Yoshimaru, Scrappy Stray")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Yoshimaru, Scrappy Stray").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            withClue("the fighter reads as a creature we lend: Craw Wurm's 6 kills Hill Giant and survives, Elves die for nothing") {
                response.selectedTargets.values.flatten() shouldBe listOf(
                    game.onBattlefield(game.player1Id, "Craw Wurm"),
                    game.onBattlefield(game.player2Id, "Hill Giant"),
                )
            }
        }
    }
}
