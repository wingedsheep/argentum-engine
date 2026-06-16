package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AbilityAutoAnsweredEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.PlayerYields
import com.wingedsheep.engine.state.YieldKind
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AbilityIdentity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for persistent per-ability yields (backlog/stack-collapse-and-batch-decisions.md §C).
 *
 * The yields live on [com.wingedsheep.engine.state.GameState] keyed by [AbilityIdentity]. These pin
 * the engine-side rules:
 *  - an `ALWAYS_ANSWER_YES` / `ALWAYS_ANSWER_NO` auto-resolves a "you may" may-question with no
 *    prompt, taking the matching branch and emitting an [AbilityAutoAnsweredEvent];
 *  - with no yield set, the may-question still prompts;
 *  - a yield is keyed by ability identity, so it never fires for a different ability;
 *  - the per-turn slice is cleared while whole-game yields/answers persist;
 *  - yields are masked per-player in the client view.
 * Auto-pass on yielded stack objects is a game-server (`AutoPassManager`) concern covered by its own
 * unit test; here we cover the engine state + the auto-answer resolution path.
 */
class PersistentYieldsTest : ScenarioTestBase() {

    // Fixed ability id so the test can build the exact AbilityIdentity to yield against. Scenario
    // cards use the card name as their cardDefinitionId (see ScenarioTestBase.createCard), so the
    // identity is deterministic.
    private val seerAbilityId = AbilityId("test_yield_seer_may_draw")
    private val seerIdentity = AbilityIdentity("Yield Seer", seerAbilityId)

    private fun newSeerGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Yield Seer")
        .withCardInLibrary(1, "Yield Filler")
        .withCardInLibrary(1, "Yield Filler")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        // "When this enters, you may draw a card." — a pure Gate.MayDecide may-question raised at
        // resolution, so the controller's auto-answer yield (if any) decides it without a prompt.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Yield Seer",
                manaCost = ManaCost.parse("{0}"),
                subtypes = setOf(Subtype("Wizard")),
                power = 1,
                toughness = 1,
                script = CardScript(
                    triggeredAbilities = listOf(
                        TriggeredAbility(
                            id = seerAbilityId,
                            trigger = Triggers.EntersBattlefield.event,
                            binding = Triggers.EntersBattlefield.binding,
                            effect = GatedEffect(gate = Gate.MayDecide(), then = DrawCardsEffect(1))
                        )
                    )
                )
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Yield Filler", manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype("Golem")), power = 1, toughness = 1
            )
        )

        test("ALWAYS_ANSWER_YES auto-resolves the may-question and draws without prompting") {
            val game = newSeerGame()
            game.state = game.state.withYield(game.player1Id, seerIdentity, YieldKind.ALWAYS_ANSWER_YES)

            val libBefore = game.librarySize(1)
            game.castSpell(1, "Yield Seer").error shouldBe null
            val events = game.resolveStack().flatMap { it.events }

            game.hasPendingDecision().shouldBeFalse()
            game.librarySize(1) shouldBe libBefore - 1 // drew a card
            events.any { it is AbilityAutoAnsweredEvent && it.answer } shouldBe true
        }

        test("ALWAYS_ANSWER_NO auto-declines the may-question with no draw") {
            val game = newSeerGame()
            game.state = game.state.withYield(game.player1Id, seerIdentity, YieldKind.ALWAYS_ANSWER_NO)

            val libBefore = game.librarySize(1)
            game.castSpell(1, "Yield Seer").error shouldBe null
            val events = game.resolveStack().flatMap { it.events }

            game.hasPendingDecision().shouldBeFalse()
            game.librarySize(1) shouldBe libBefore // declined → no draw
            events.any { it is AbilityAutoAnsweredEvent && !it.answer } shouldBe true
        }

        test("with no yield set, the may-question still prompts") {
            val game = newSeerGame()
            game.castSpell(1, "Yield Seer").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        }

        test("an auto-answer yield is keyed by identity, so it does not fire for a different ability") {
            val game = newSeerGame()
            game.state = game.state.withYield(
                game.player1Id,
                AbilityIdentity("Some Other Card", AbilityId("other")),
                YieldKind.ALWAYS_ANSWER_YES,
            )
            game.castSpell(1, "Yield Seer").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        }

        test("yields are masked per-player in the client view") {
            val game = newSeerGame()
            game.state = game.state.withYield(game.player1Id, seerIdentity, YieldKind.YIELD_WHOLE_GAME)

            val p1View = game.getClientState(1).activeYields
            p1View.map { it.cardDefinitionId } shouldContainExactly listOf("Yield Seer")
            p1View.single().wholeGame shouldBe true
            p1View.single().displayName shouldBe "Yield Seer"

            // Player 2 must never see player 1's yields.
            game.getClientState(2).activeYields.shouldBeEmpty()
        }

        test("GameState yield helpers set, query, revoke, and clear-all") {
            val base = newSeerGame().state
            val p = base.turnOrder.first()
            val other = AbilityIdentity("Other", AbilityId("o"))

            val withYields = base
                .withYield(p, seerIdentity, YieldKind.YIELD_UNTIL_END_OF_TURN)
                .withYield(p, other, YieldKind.ALWAYS_ANSWER_NO)

            withYields.isYieldingTo(p, seerIdentity).shouldBeTrue()
            withYields.autoAnswerFor(p, other) shouldBe false

            // Turn-boundary clearing drops the until-end-of-turn auto-pass but keeps the auto-answer.
            val afterCleanup = withYields.clearUntilEndOfTurnYields()
            afterCleanup.isYieldingTo(p, seerIdentity).shouldBeFalse()
            afterCleanup.autoAnswerFor(p, other) shouldBe false

            // Revoke + clear-all.
            afterCleanup.withoutYield(p, other).autoAnswerFor(p, other) shouldBe null
            withYields.withoutYields(p).yieldsFor(p) shouldBe PlayerYields.EMPTY
        }
    }
}
