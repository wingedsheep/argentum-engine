package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hidden Strings (DGM #12) — {1}{U} Sorcery.
 *
 *   You may tap or untap target permanent, then you may tap or untap another target permanent.
 *   Cipher
 *
 * Two distinct targets, each with its own optional tap-or-untap choice, then the encode step.
 */
class HiddenStringsScenarioTest : ScenarioTestBase() {
    init {
        fun board(): TestGame = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Hidden Strings")
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardOnBattlefield(1, "Hill Giant", tapped = true)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.cast(first: EntityId, second: EntityId) = execute(
            CastSpell(
                player1Id,
                state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Hidden Strings" },
                listOf(ChosenTarget.Permanent(first), ChosenTarget.Permanent(second)),
            )
        )

        fun TestGame.choose(prefix: String) {
            val mode = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            submitDecision(OptionChosenResponse(mode.id, mode.options.indexOfFirst { it.startsWith(prefix) }))
                .error shouldBe null
        }

        fun TestGame.tapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

        test("taps one target and untaps the other, then encodes on a creature") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.cast(bears, giant).error shouldBe null
            game.resolveStack()

            game.answerYesNo(true).error shouldBe null
            game.choose("Tap")
            game.answerYesNo(true).error shouldBe null
            game.choose("Untap")

            withClue("the opponent's Bears are tapped and your Giant is untapped") {
                game.tapped(bears) shouldBe true
                game.tapped(giant) shouldBe false
            }

            game.selectCards(listOf(giant)).error shouldBe null
            game.isInExile(1, "Hidden Strings") shouldBe true
        }

        test("each half is optional on its own") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.cast(bears, giant).error shouldBe null
            game.resolveStack()

            game.answerYesNo(false).error shouldBe null
            game.answerYesNo(true).error shouldBe null
            game.choose("Untap")

            withClue("declining the first half leaves the Bears untapped; the Giant still untaps") {
                game.tapped(bears) shouldBe false
                game.tapped(giant) shouldBe false
            }
        }

        test("the second target must be another permanent") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.cast(bears, bears).error shouldNotBe null
        }
    }
}
