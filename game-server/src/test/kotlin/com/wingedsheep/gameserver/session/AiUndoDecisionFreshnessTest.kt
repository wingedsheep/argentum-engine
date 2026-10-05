package com.wingedsheep.gameserver.session

import com.wingedsheep.ai.ActionResponse
import com.wingedsheep.ai.AiPlayerController
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CancelDecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.ai.AiWebSocketSession
import com.wingedsheep.gameserver.handler.GamePlayHandler
import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger

class AiUndoDecisionFreshnessTest : ScenarioTestBase() {
    init {
        test("an actual delayed AI callback cannot answer the replacement Closet target decision after undo") {
            val game = scenario().withPlayers("Human", "AI")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardInHand(1, "Naturalize")
                .withCardOnBattlefield(2, "Conjurer's Closet")
                .withCardOnBattlefield(2, "Conjurer's Closet")
                .withCardOnBattlefield(2, "Conjurer's Closet")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()
            val human = game.player1Id
            val ai = game.player2Id
            val session = spyk(GameSession(cardRegistry = cardRegistry))
            val sender = mockk<MessageSender>(relaxed = true)
            val handler = handler(sender)
            val oldChoosing = CountDownLatch(1)
            val newChoosing = CountDownLatch(1)
            val releaseOld = CountDownLatch(1)
            val releaseNew = CountDownLatch(1)
            val oldReturned = CountDownLatch(1)
            val newReturned = CountDownLatch(1)
            val callbackFailure = AtomicReference<Throwable?>()
            val oldEpoch = AtomicReference<String?>()
            val newEpoch = AtomicReference<String?>()
            val oldAction = AtomicReference<GameAction?>()
            val newAction = AtomicReference<GameAction?>()
            val oldTarget = game.findPermanent("Grizzly Bears")!!
            val newTarget = game.findPermanent("Hill Giant")!!
            val choices = AtomicInteger()
            val controller = mockk<AiPlayerController>()
            every { controller.chooseAction(any(), any(), any(), any()) } answers {
                val decision = thirdArg<com.wingedsheep.engine.core.PendingDecision>()
                    .shouldBeInstanceOf<ChooseTargetsDecision>()
                val old = choices.getAndIncrement() == 0
                if (old) {
                    oldChoosing.countDown()
                    await(releaseOld, "release old controller response")
                } else {
                    newChoosing.countDown()
                    await(releaseNew, "release new controller response")
                }
                // Both choices are legal; the payload distinguishes callbacks independently of epochs.
                ActionResponse.SubmitDecision(ai, TargetsResponse(
                    decision.id, selectedTargets = mapOf(0 to listOf(if (old) oldTarget else newTarget))
                ))
            }
            val aiSocket = AiWebSocketSession(
                aiPlayerId = ai,
                controller = controller,
                thinkingDelayMs = 0,
                onActionReady = { player, action, epoch ->
                    val response = (action as SubmitDecision).response as TargetsResponse
                    val old = response.selectedTargets[0] == listOf(oldTarget)
                    try {
                        if (old) {
                            oldEpoch.set(epoch)
                            oldAction.set(action)
                        } else {
                            newEpoch.set(epoch)
                            newAction.set(action)
                        }
                        handler.handleAiAction(session, player, action, epoch)
                    } catch (failure: Throwable) {
                        callbackFailure.compareAndSet(null, failure)
                    } finally {
                        (if (old) oldReturned else newReturned).countDown()
                    }
                },
                onMulliganKeep = {},
                onMulliganTake = {},
                onBottomCards = { _, _ -> }
            )
            val humanSocket = mockk<WebSocketSession>(relaxed = true) { every { id } returns "human" }
            session.injectStateForTesting(game.state, mapOf(
                human to PlayerSession(humanSocket, human, "Human"),
                ai to PlayerSession(aiSocket, ai, "AI")
            ))
            session.setFullControl(human, true)
            session.setFullControl(ai, true)
            val json = Json {
                serializersModule = engineSerializersModule
                classDiscriminator = "type"
                encodeDefaults = true
            }

            try {
                val checkpoint = session.getStateForTesting()!!
                // The undo policy recognizes declared mana abilities in the card script;
                // intrinsic basic-land mana currently does not establish this checkpoint.
                val manaSource = game.findPermanent("Llanowar Elves")!!
                val manaAbility = cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities
                    .single { it.isManaAbility }.id
                session.executeAction(human, ActivateAbility(human, manaSource, manaAbility))
                    .shouldBeInstanceOf<GameSession.ActionResult.Success>()
                session.isUndoAvailable(human) shouldBe true
                val oldTargets = advanceToTargets(session)
                session.getStateForTesting()!!.continuationStack
                    .filterIsInstance<com.wingedsheep.engine.core.PendingTriggersContinuation>()
                    .single().remainingTriggers.size shouldBe 2
                session.isUndoAvailable(human) shouldBe true
                val oldUpdate = session.createStateUpdate(ai, emptyList(), useEngineDecisionIds = true)
                    .shouldBeInstanceOf<ServerMessage.StateUpdate>()
                oldUpdate.interactionEpoch.shouldNotBeNull()
                aiSocket.sendMessage(TextMessage(json.encodeToString<ServerMessage>(oldUpdate)))
                await(oldChoosing, "old AI controller to receive the first of three target decisions")

                session.executeUndo(human).shouldBeInstanceOf<GameSession.ActionResult.Success>()
                session.getStateForTesting() shouldBe checkpoint
                val closet = game.findPermanents("Conjurer's Closet").first()
                val naturalize = game.findCardsInHand(1, "Naturalize").single()
                session.executeAction(human, CastSpell(human, naturalize, listOf(ChosenTarget.Permanent(closet))))
                    .shouldBeInstanceOf<GameSession.ActionResult.Success>()
                val replacementTargets = advanceToTargets(session)
                session.getStateForTesting()!!.continuationStack
                    .filterIsInstance<com.wingedsheep.engine.core.PendingTriggersContinuation>()
                    .single().remainingTriggers.size shouldBe 1
                replacementTargets.id shouldBe oldTargets.id
                val newUpdate = session.createStateUpdate(ai, emptyList(), useEngineDecisionIds = true)
                    .shouldBeInstanceOf<ServerMessage.StateDeltaUpdate>()
                newUpdate.interactionEpoch.shouldNotBeNull()
                newUpdate.interactionEpoch shouldNotBe oldUpdate.interactionEpoch
                aiSocket.sendMessage(TextMessage(json.encodeToString<ServerMessage>(newUpdate)))
                await(newChoosing, "new AI controller to receive the first of two target decisions")

                val stateBefore = session.getStateForTesting()
                val actionsBefore = session.getRecordedActions()
                val checkpointsBefore = session.getReplayCheckpoints()
                val logsBefore = session.getLogsForPersistence()
                val undoBefore = session.isUndoAvailable(human)
                clearMocks(session, sender, answers = false)
                releaseOld.countDown()
                await(oldReturned, "obsolete callback to finish through GamePlayHandler")
                callbackFailure.get() shouldBe null
                oldEpoch.get() shouldBe oldUpdate.interactionEpoch
                oldAction.get().shouldBeInstanceOf<SubmitDecision>().response.decisionId shouldBe replacementTargets.id
                session.getStateForTesting() shouldBe stateBefore
                session.getRecordedActions() shouldBe actionsBefore
                session.getReplayCheckpoints() shouldBe checkpointsBefore
                session.getLogsForPersistence() shouldBe logsBefore
                session.isUndoAvailable(human) shouldBe undoBefore
                verify(exactly = 0) { session.executeAction(any(), any(), any()) }
                verify(exactly = 0) { session.noteActionRejected(any()) }
                verify(exactly = 0) { session.noteAiActionRejected(any(), any()) }
                verify(exactly = 0) { sender.send(any(), any()) }

                releaseNew.countDown()
                await(newReturned, "current callback to finish through GamePlayHandler")
                callbackFailure.get() shouldBe null
                newEpoch.get() shouldBe newUpdate.interactionEpoch
                val accepted = newAction.get().shouldBeInstanceOf<SubmitDecision>()
                accepted.response.decisionId shouldBe replacementTargets.id
                session.getRecordedActions() shouldBe actionsBefore + accepted
                session.getStateForTesting() shouldNotBe stateBefore
                val replay = actionProcessor.process(stateBefore!!, accepted).result
                replay.error shouldBe null
                session.getStateForTesting() shouldBe replay.state
                verify(exactly = 0) { session.noteActionRejected(any()) }
                verify(exactly = 0) { session.noteAiActionRejected(any(), any()) }
            } finally {
                releaseOld.countDown()
                releaseNew.countDown()
                aiSocket.close()
            }
        }

        test("undo after a current AI action fails abandons its fallback and rejection bookkeeping") {
            val game = scenario().withPlayers("Human", "AI")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Conjurer's Closet")
                .withCardOnBattlefield(2, "Conjurer's Closet")
                .withCardOnBattlefield(2, "Conjurer's Closet")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()
            val human = game.player1Id
            val ai = game.player2Id
            val session = spyk(GameSession(cardRegistry = cardRegistry))
            val humanSocket = mockk<WebSocketSession>(relaxed = true) { every { id } returns "human" }
            val aiSocket = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ai" }
            session.injectStateForTesting(game.state, mapOf(
                human to PlayerSession(humanSocket, human, "Human"),
                ai to PlayerSession(aiSocket, ai, "AI")
            ))
            val manaSource = game.findPermanent("Llanowar Elves")!!
            val manaAbility = cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities
                .single { it.isManaAbility }.id
            val activate = ActivateAbility(human, manaSource, manaAbility)
            session.executeAction(human, activate).shouldBeInstanceOf<GameSession.ActionResult.Success>()
            val original = advanceToTargets(session)
            session.isUndoAvailable(human) shouldBe true
            val update = session.createStateUpdate(ai, emptyList(), useEngineDecisionIds = true)
                .shouldBeInstanceOf<ServerMessage.StateUpdate>()
            val epoch = update.interactionEpoch.shouldNotBeNull()
            val invalid = PassPriority(ai) // A priority pass cannot answer the outstanding target decision.
            val sender = mockk<MessageSender>(relaxed = true)
            val handler = handler(sender)
            var replacementState: GameState? = null
            var replacementActions = emptyList<GameAction>()
            var replacementEpoch: String? = null
            var interleaved = false
            every { session.executeAiAction(ai, invalid, epoch) } answers {
                val result = callOriginal()
                if (!interleaved) {
                    interleaved = true
                    result.shouldBeInstanceOf<GameSession.ActionResult.Failure>()
                    // Place the undo precisely after the first atomic validation/execution but
                    // before GamePlayHandler sees Failure and begins its recovery path.
                    session.executeUndo(human).shouldBeInstanceOf<GameSession.ActionResult.Success>()
                    session.executeAction(human, activate).shouldBeInstanceOf<GameSession.ActionResult.Success>()
                    advanceToTargets(session).id shouldBe original.id
                    replacementState = session.getStateForTesting()
                    replacementActions = session.getRecordedActions()
                    replacementEpoch = session.createStateUpdate(ai, emptyList(), useEngineDecisionIds = true)
                        .shouldBeInstanceOf<ServerMessage.StateDeltaUpdate>().interactionEpoch
                }
                result
            }
            val logsBefore = session.getLogsForPersistence()
            val checkpointsBefore = session.getReplayCheckpoints()
            handler.handleAiAction(session, ai, invalid, epoch)
            interleaved shouldBe true
            replacementEpoch.shouldNotBeNull() shouldNotBe epoch
            session.getStateForTesting() shouldBe replacementState
            session.getRecordedActions() shouldBe replacementActions
            session.getReplayCheckpoints() shouldBe checkpointsBefore
            session.getLogsForPersistence() shouldBe logsBefore
            session.isUndoAvailable(human) shouldBe true
            verify(exactly = 0) {
                session.executeAction(ai, match { it is SubmitDecision && it.response is CancelDecisionResponse }, any())
            }
            verify(exactly = 0) { session.noteActionRejected(any()) }
            verify(exactly = 0) { session.noteAiActionRejected(any(), any()) }
            verify(exactly = 0) { sender.send(any(), any()) }
        }
    }

    private fun advanceToTargets(session: GameSession): ChooseTargetsDecision {
        repeat(12) {
            val state = session.getStateForTesting()!!
            state.pendingDecision?.let { return it.shouldBeInstanceOf<ChooseTargetsDecision>() }
            val player = state.priorityPlayerId!!
            val result = session.executeAction(player, PassPriority(player))
            check(result !is GameSession.ActionResult.Failure) { "Priority pass failed: $result" }
        }
        error("End-step target decision was not reached within 12 priority passes")
    }

    private fun await(latch: CountDownLatch, description: String) {
        check(latch.await(15, TimeUnit.SECONDS)) { "Timed out waiting for $description" }
    }

    private fun handler(sender: MessageSender) = GamePlayHandler(
        sessionRegistry = mockk(relaxed = true),
        gameRepository = mockk(relaxed = true),
        lobbyRepository = mockk(relaxed = true),
        sender = sender,
        cardRegistry = cardRegistry,
        printingRegistry = mockk(relaxed = true),
        tokenArtRegistry = mockk(relaxed = true),
        deckGenerator = mockk(relaxed = true),
        gameProperties = mockk(relaxed = true),
        replayService = mockk(relaxed = true),
        replayCheckpointFlusher = mockk(relaxed = true),
        engineVersion = mockk(relaxed = true),
        aiGameManager = mockk(relaxed = true),
        matchResultSink = mockk(relaxed = true),
        rankedResultSink = mockk(relaxed = true),
        deckProfiler = mockk(relaxed = true)
    )
}
