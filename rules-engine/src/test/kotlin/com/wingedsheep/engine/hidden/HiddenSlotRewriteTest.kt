package com.wingedsheep.engine.hidden

import com.wingedsheep.engine.core.ContinuationFrame
import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MadnessComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.TypedEntityReferences
import com.wingedsheep.engine.core.InFlightReferenceProjector
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class HiddenSlotRewriteTest : ScenarioTestBase() {

    init {
        test("runtime blockers retain identity exclusions, decoration values, and sorted names") {
            val owner = EntityId.of("owner")
            val definition = cardRegistry.requireCard("Fiery Temper")
            val expected = CardEntityFactory.create(definition, owner)
            val renamed = expected.with(expected.require<CardComponent>().copy(name = "Printed alias"))
            HiddenSlotRewrite.runtimeBlockers(renamed, definition, owner) shouldBe emptyList()

            val changed = renamed
                .with(RevealedToComponent.to(EntityId.of("viewer")))
                .with(MadnessComponent(ManaCost.parse("{7}")))
            HiddenSlotRewrite.runtimeBlockers(changed, definition, owner) shouldBe
                listOf("MadnessComponent", "RevealedToComponent")
            expected.require<MadnessComponent>().cost shouldNotBe ManaCost.parse("{7}")
        }

        test("a Mind Rot paused graph pins its discard options but not an unrelated library slot") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Mind Rot")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Swamp")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Craw Wurm")
                .withCardInLibrary(2, "Forest")
                .build()
            game.castSpellTargetingPlayer(1, "Mind Rot", 2).error shouldBe null
            game.resolveStack()

            val source = game.state
            val decision = source.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            val libraryId = source.getLibrary(game.player2Id).single()

            val pins = HiddenSlotRewrite.identitySensitiveInFlightPins(source)
                .shouldBeInstanceOf<HiddenSlotRewrite.IdentitySensitiveInFlightPins.Complete>()

            decision.options.forEach { pins.entityIds shouldContain it }
            pins.entityIds shouldNotContain libraryId
        }

        // The library axis of the same rule Mind Rot covers for hands. This is the case main
        // refused wholesale: any continuation frame at all pinned every candidate slot.
        test("a Preordain scry pins the library slot it offers but not one buried below it") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Preordain")
                .withCardOnBattlefield(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Grizzly Bears")
                .build()
            game.castSpell(1, "Preordain").error shouldBe null
            game.resolveStack()

            // Preordain scries before it draws, so the engine is parked on a decision naming the
            // top of the library with the rest of the resolution held in a continuation frame.
            val source = game.state
            source.pendingDecision shouldNotBe null
            source.continuationStack.isNotEmpty() shouldBe true
            val library = source.getLibrary(game.player1Id)

            val pins = HiddenSlotRewrite.identitySensitiveInFlightPins(source)
                .shouldBeInstanceOf<HiddenSlotRewrite.IdentitySensitiveInFlightPins.Complete>()

            pins.entityIds shouldContain library.first()
            pins.entityIds shouldNotContain library.last()
        }

        test("an incomplete paused projection is the shared in-flight answer") {
            val game = scenario().withPlayers().build()
            val state = game.state.copy(
                pendingDecision = SelectCardsDecision(
                    id = "untraversable",
                    playerId = game.player1Id,
                    prompt = "choose",
                    context = DecisionContext(),
                    options = emptyList(),
                    minSelections = 0,
                    maxSelections = 0,
                ),
            )

            HiddenSlotRewrite.identitySensitiveInFlightPins(state, projectorFailingOn(Root.DECISION))
                .shouldBeInstanceOf<HiddenSlotRewrite.IdentitySensitiveInFlightPins.Incomplete>()
                .reason shouldBe "could not traverse pending decision test: forced"
        }

        test("a missing or untraversable stack object makes the shared pin answer incomplete") {
            val game = scenario().withPlayers().build()
            val missingStackId = EntityId.of("missing-stack-object")

            HiddenSlotRewrite.identitySensitiveInFlightPins(
                game.state.copy(stack = listOf(missingStackId)),
            ).shouldBeInstanceOf<HiddenSlotRewrite.IdentitySensitiveInFlightPins.Incomplete>()
                .reason shouldBe "could not traverse stack[0] missing-stack-object: missing entity"

            val stackId = EntityId.of("untraversable-stack-object")
            val withStack = game.state.copy(
                entities = game.state.entities + (stackId to ComponentContainer.EMPTY),
                stack = listOf(stackId),
            )
            HiddenSlotRewrite.identitySensitiveInFlightPins(withStack, projectorFailingOn(Root.STACK))
                .shouldBeInstanceOf<HiddenSlotRewrite.IdentitySensitiveInFlightPins.Incomplete>()
                .reason shouldBe "could not traverse stack[0] test: forced"
        }
    }

    private enum class Root { STACK, DECISION }

    /** A projector that traverses everything but [failing], which reports an incomplete graph. */
    private fun projectorFailingOn(failing: Root) = object : InFlightReferenceProjector {
        override fun project(stackObject: ComponentContainer) = projection(Root.STACK)
        override fun project(decision: PendingDecision) = projection(Root.DECISION)
        override fun project(frame: ContinuationFrame) =
            TypedEntityReferences.Projection.Complete(emptyList())

        private fun projection(root: Root) =
            if (root == failing) TypedEntityReferences.Projection.Incomplete("test", "forced")
            else TypedEntityReferences.Projection.Complete(emptyList())
    }
}
