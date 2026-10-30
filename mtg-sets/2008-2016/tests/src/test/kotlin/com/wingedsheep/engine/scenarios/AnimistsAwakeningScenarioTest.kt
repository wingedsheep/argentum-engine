package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Animist's Awakening (ORI #169) — reveal the top X, lands onto the battlefield tapped, the rest on
 * the bottom; with spell mastery, untap only the lands it put onto the battlefield.
 */
class AnimistsAwakeningScenarioTest : ScenarioTestBase() {

    init {
        context("Animist's Awakening") {

            fun board(spellMastery: Boolean): TestGame {
                val builder = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Animist's Awakening")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                if (spellMastery) {
                    builder.withCardInGraveyard(1, "Lightning Bolt")
                    builder.withCardInGraveyard(1, "Opt")
                }
                return builder.build()
            }

            fun TestGame.tapped(id: com.wingedsheep.sdk.model.EntityId) =
                state.getEntity(id)?.has<TappedComponent>() == true

            test("puts every revealed land onto the battlefield tapped and the rest on the bottom") {
                val game = board(spellMastery = false)
                game.castXSpell(1, "Animist's Awakening", xValue = 4).error shouldBe null
                game.resolveStack()

                val mountains = game.findAllPermanents("Mountain")
                mountains shouldHaveSize 2
                withClue("no spell mastery: the lands stay tapped") {
                    mountains.all { game.tapped(it) } shouldBe true
                }
                withClue("the two nonland cards went back into the library") {
                    game.librarySize(1) shouldBe 2
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("spell mastery untaps only the lands it put onto the battlefield") {
                val game = board(spellMastery = true)
                game.castXSpell(1, "Animist's Awakening", xValue = 4).error shouldBe null
                game.resolveStack()

                val mountains = game.findAllPermanents("Mountain")
                mountains shouldHaveSize 2
                withClue("the new lands are untapped") {
                    mountains.none { game.tapped(it) } shouldBe true
                }
                withClue("the Forests tapped to cast it stay tapped") {
                    game.findAllPermanents("Forest").all { game.tapped(it) } shouldBe true
                }
            }
        }
    }
}
