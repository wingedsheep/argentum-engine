package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * First Contact (TRK #199) — {1}{G} Instant.
 *
 *   "Mill four cards. You may put a permanent card from among them into your hand. If this spell is
 *    the first spell you've cast this game, you gain 2 life."
 *
 * "First spell" is judged as of the cast, so a spell cast after it — even one cast in response that
 * resolves first — doesn't take the life away, while a spell cast earlier does. A copy isn't cast
 * (CR 707.10), so it is never the first spell.
 */
class FirstContactScenarioTest : ScenarioTestBase() {

    /** A plain "copy target instant or sorcery spell" for the copy case. */
    private val testCopier = card("Test Copier") {
        manaCost = "{1}"
        typeLine = "Instant"
        oracleText = "Copy target instant or sorcery spell. You may choose new targets for the copy."
        spell {
            val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
            effect = Effects.CopyTargetSpell(spell)
        }
    }

    private fun newGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "First Contact")
        .withCardInHand(1, "Lightning Bolt")
        .withCardInHand(1, "Test Copier")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(1, "Sol Ring")
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(testCopier)

        test("as the first spell of the game it mills four, returns a permanent card and gains 2 life") {
            val game = newGame()
            game.castSpell(1, "First Contact").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision as? SelectCardsDecision
            withClue("the optional pick pauses for a selection") { decision shouldNotBe null }
            withClue("four cards were milled") { game.graveyardSize(1) shouldBe 4 }
            withClue("only permanent cards can be picked — the milled Lightning Bolt can't") {
                decision!!.options shouldNotContain game.findCardsInGraveyard(1, "Lightning Bolt").single()
            }
            game.selectCards(game.findCardsInGraveyard(1, "Grizzly Bears"))

            withClue("the chosen permanent card goes to hand") { game.isInHand(1, "Grizzly Bears") shouldBe true }
            withClue("the instant Lightning Bolt stays milled") { game.isInGraveyard(1, "Lightning Bolt") shouldBe true }
            withClue("first spell this game: gain 2 life") { game.getLifeTotal(1) shouldBe 22 }
        }

        test("cast after another spell, it gains no life") {
            val game = newGame()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17

            game.castSpell(1, "First Contact").error shouldBe null
            game.resolveStack()
            game.skipSelection()

            withClue("four milled cards, the Bolt and First Contact itself are in the graveyard") { game.graveyardSize(1) shouldBe 6 }
            withClue("not the first spell cast this game: no life") { game.getLifeTotal(1) shouldBe 20 }
        }

        test("a spell cast in response doesn't stop it from being the first spell") {
            val game = newGame()
            game.castSpell(1, "First Contact").error shouldBe null
            // Holding priority, the Bolt goes on top and resolves first.
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            withClue("the Bolt resolved first") { game.getLifeTotal(2) shouldBe 17 }

            game.skipSelection()
            withClue("First Contact was still the first spell cast this game") { game.getLifeTotal(1) shouldBe 22 }
        }

        test("a copy of the first spell isn't cast, so only the original gains life") {
            val game = newGame()
            game.castSpell(1, "First Contact").error shouldBe null
            game.castSpellTargetingStackSpell(1, "Test Copier", "First Contact").error shouldBe null
            game.resolveStack()

            // The copy resolves first and asks for its pick; then the original asks for its own.
            (game.state.pendingDecision as? SelectCardsDecision) shouldNotBe null
            game.skipSelection()
            withClue("the copy wasn't cast: no life") { game.getLifeTotal(1) shouldBe 20 }
            game.resolveStack()
            (game.state.pendingDecision as? SelectCardsDecision) shouldNotBe null
            game.skipSelection()
            withClue("the original was the first spell cast this game: gain exactly 2") { game.getLifeTotal(1) shouldBe 22 }
        }
    }
}
