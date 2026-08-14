package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Molten Man, Inferno Incarnate — {2}{R} Legendary Creature — Elemental Villain, 0/0.
 *
 *  - "When Molten Man enters, search your library for a basic Mountain card, put it onto
 *     the battlefield tapped, then shuffle."
 *  - "Molten Man gets +1/+1 for each Mountain you control."
 *  - "When Molten Man leaves the battlefield, sacrifice a land."
 */
class MoltenManInfernoIncarnateScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("ETB searches for a basic Mountain (only) and puts it onto the battlefield tapped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Molten Man, Inferno Incarnate")
                .withLandsOnBattlefield(1, "Mountain", 3) // provides {2}{R}
                .withCardInLibrary(1, "Mountain")         // the only legal fetch target
                .withCardInLibrary(1, "Forest")           // a basic that must be excluded
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val libraryMountain = game.findCardsInLibrary(1, "Mountain").first()

            val castResult = game.castSpell(1, "Molten Man, Inferno Incarnate")
            withClue("Molten Man should cast: ${castResult.error}") { castResult.error shouldBe null }

            // Resolve the creature spell; the ETB trigger then pauses on the library search.
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("Only the basic Mountain is searchable; the Forest is excluded") {
                decision.options shouldContainExactly listOf(libraryMountain)
            }

            game.selectCards(listOf(libraryMountain))

            withClue("The fetched Mountain is now on the battlefield") {
                game.state.getBattlefield().contains(libraryMountain) shouldBe true
            }
            withClue("The fetched Mountain enters tapped") {
                game.state.getEntity(libraryMountain)?.has<TappedComponent>() shouldBe true
            }
        }

        test("gets +1/+1 for each Mountain you control and scales as Mountains enter") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Molten Man, Inferno Incarnate")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val molten = game.findPermanent("Molten Man, Inferno Incarnate").shouldNotBeNull()

            // Base 0/0 + 2 Mountains = 2/2.
            withClue("power with 2 Mountains (0 + 2)") {
                stateProjector.getProjectedPower(game.state, molten) shouldBe 2
            }
            withClue("toughness with 2 Mountains (0 + 2)") {
                stateProjector.getProjectedToughness(game.state, molten) shouldBe 2
            }

            // Play a 3rd Mountain from hand — the buff scales dynamically.
            val mountainInHand = game.findCardsInHand(1, "Mountain").first()
            val playResult = game.execute(PlayLand(game.player1Id, mountainInHand))
            withClue("Playing a 3rd Mountain should succeed: ${playResult.error}") {
                playResult.error shouldBe null
            }

            withClue("power with 3 Mountains (0 + 3)") {
                stateProjector.getProjectedPower(game.state, molten) shouldBe 3
            }
            withClue("toughness with 3 Mountains (0 + 3)") {
                stateProjector.getProjectedToughness(game.state, molten) shouldBe 3
            }
        }

        test("leaving the battlefield forces its controller to sacrifice a land") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Molten Man, Inferno Incarnate")
                .withLandsOnBattlefield(1, "Mountain", 2) // keeps it 2/2 alive + sac fodder
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)     // mana for Doom Blade {1}{B}
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val molten = game.findPermanent("Molten Man, Inferno Incarnate").shouldNotBeNull()

            // Destroy Molten Man with its controller's own removal (it's red, so a legal target).
            val castResult = game.castSpell(1, "Doom Blade", molten)
            withClue("Doom Blade should cast: ${castResult.error}") { castResult.error shouldBe null }

            // Resolving Doom Blade destroys Molten Man; its leaves trigger then pauses on the
            // "sacrifice a land" choice.
            game.resolveStack()

            withClue("Molten Man should be in the graveyard") {
                game.isInGraveyard(1, "Molten Man, Inferno Incarnate") shouldBe true
            }

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()

            val swamp = game.findPermanents("Swamp").first()
            game.selectCards(listOf(swamp))

            withClue("The chosen land is sacrificed to the graveyard") {
                game.isInGraveyard(1, "Swamp") shouldBe true
            }
            withClue("Only one Swamp remains on the battlefield") {
                game.findPermanents("Swamp").size shouldBe 1
            }
        }
    }
}
