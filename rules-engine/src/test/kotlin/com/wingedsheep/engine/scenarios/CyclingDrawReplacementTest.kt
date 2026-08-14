package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ons.cards.WordsOfWorship
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A draw whose call site was built without an effect executor must not silently swallow the
 * draw. `CycleCardHandler` constructs `DrawCardsExecutor(cardRegistry = cardRegistry)` with no
 * `effectExecutor`, so `DrawReplacementDispatcher.executeReplacement` short-circuits on
 * `effectExecutor ?: return DispatchResult.Replaced(...)` — reporting the draw as replaced
 * without running the replacement or consuming the shield.
 *
 * On `main` a null executor made the dispatcher skip the shield check entirely
 * (`shieldConsumer = effectExecutor?.let { ... }`), so the card was drawn normally.
 */
class CyclingDrawReplacementTest : ScenarioTestBase() {

    init {

        // =====================================================================
        // A draw whose call site has no effect executor must not swallow the draw.
        //
        // CycleCardHandler builds `DrawCardsExecutor(cardRegistry = cardRegistry)` with no
        // effectExecutor, so DrawReplacementDispatcher.executeReplacement short-circuits on
        // `effectExecutor ?: return DispatchResult.Replaced(...)` — reporting the draw as
        // replaced without running the replacement or consuming the shield.
        // =====================================================================

        test("cycling a card with a Words of Worship shield active gains 5 life instead of drawing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Words of Worship")
                .withCardInHand(1, "Fade from Memory")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val worship = game.findPermanent("Words of Worship")!!
            val abilityId = WordsOfWorship.activatedAbilities.first().id

            // "{1}: The next time you would draw a card this turn, you gain 5 life instead."
            val activation = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = worship,
                    abilityId = abilityId,
                    targets = emptyList()
                )
            )
            withClue("Activating Words of Worship should succeed: ${activation.error}") {
                activation.error shouldBe null
            }
            game.resolveStack()

            withClue("Activation installs a Duration.NextUse draw-replacement shield") {
                game.state.floatingEffects.count {
                    it.effect.modification is SerializableModification.ReplaceDrawWithEffect
                } shouldBe 1
            }

            val lifeBefore = game.getLifeTotal(1)
            val libraryBefore = game.librarySize(1)

            // Cycling is "Discard this card: Draw a card" (CR 702.29a) — that draw is a
            // draw event, so the shield replaces it.
            val cycle = game.cycleCard(1, "Fade from Memory")
            withClue("Cycling should succeed: ${cycle.error}") {
                cycle.error shouldBe null
            }
            game.resolveStack()

            withClue("Fade from Memory was discarded to pay the cycling cost") {
                game.isInGraveyard(1, "Fade from Memory") shouldBe true
            }
            withClue(
                "The shield replaced the cycling draw with 5 life gain. Staying at " +
                    "$lifeBefore means the draw was reported as replaced but the stored " +
                    "effect never ran — the draw was silently swallowed."
            ) {
                game.getLifeTotal(1) shouldBe lifeBefore + 5
            }
            withClue("The replacement ran instead of the draw, so the library is untouched") {
                game.librarySize(1) shouldBe libraryBefore
            }
            withClue("A Duration.NextUse shield is consumed once it applies") {
                game.state.floatingEffects.count {
                    it.effect.modification is SerializableModification.ReplaceDrawWithEffect
                } shouldBe 0
            }
        }
    }
}
