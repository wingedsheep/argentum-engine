package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Legolas, Counter of Kills (LTR #212) — "Whenever you scry, if Legolas is tapped, you may untap
 * it. **Do this only once each turn.**"
 *
 * The card is the cleanest statement of the `effectOncePerTurn` rider there is, because its
 * official ruling spells out both halves of CR 603.2h in one sentence:
 *
 * > "Do this only once each turn" lets you choose whether or not to untap Legolas as the triggered
 * > ability resolves. If you don't untap it, the ability will trigger again the next time the
 * > condition is met. Once you choose to do so, the ability will no longer trigger for the rest of
 * > the turn.
 *
 * So the turn's single use is spent by the *untap*, not by the trigger. These tests pin both
 * directions: declining leaves a later scry still able to untap, and accepting stops the ability
 * for the rest of the turn. Under the trigger cap (`oncePerTurn`) the first declined scry burned
 * the turn — the defect this card was migrated off.
 *
 * The intervening-if (`Conditions.SourceIsTapped`, CR 603.4) is checked independently: an untapped
 * Legolas offers nothing at all, which is also why accepting is self-limiting.
 */
class LegolasCounterOfKillsScenarioTest : ScenarioTestBase() {

    init {
        // A minimal scry source we can cast twice in one turn.
        val scrySpell = card("Test Scry Spell") {
            manaCost = "{U}"
            typeLine = "Instant"
            oracleText = "Scry 1."
            spell { effect = Effects.Scry(1) }
        }
        cardRegistry.register(listOf(scrySpell))

        /** Resolve a scry's "which cards to the bottom" + "reorder the top" prompts. */
        fun TestGame.answerScry() {
            skipSelection()
            keepLibraryOrder()
        }

        fun scenarioWithTappedLegolas(scrySpells: Int) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Legolas, Counter of Kills", tapped = true)
            .withCardsInHand(1, "Test Scry Spell", scrySpells)
            .withLandsOnBattlefield(1, "Island", 4)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("declining does not spend the turn — a later scry can still untap Legolas") {
            val game = scenarioWithTappedLegolas(scrySpells = 2)
            val legolas = game.findPermanent("Legolas, Counter of Kills")!!

            // First scry: the ability triggers and offers the untap. Decline it.
            game.castSpell(1, "Test Scry Spell").error shouldBe null
            game.resolveStack()
            game.answerScry()
            game.resolveStack()
            withClue("the first scry offers the untap") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(false)
            game.resolveStack()
            withClue("declining left Legolas tapped") {
                game.state.getEntity(legolas)!!.get<TappedComponent>().shouldNotBeNull()
            }

            // Second scry the same turn: because nothing was untapped, the ability triggers again.
            game.castSpell(1, "Test Scry Spell").error shouldBe null
            game.resolveStack()
            game.answerScry()
            game.resolveStack()
            withClue("the ability triggers again after a decline (the Legolas ruling)") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true)
            game.resolveStack()
            withClue("accepting the second offer untapped Legolas") {
                game.state.getEntity(legolas)!!.get<TappedComponent>().shouldBeNull()
            }
        }

        test("accepting spends the turn — and an untapped Legolas has nothing left to offer") {
            val game = scenarioWithTappedLegolas(scrySpells = 2)
            val legolas = game.findPermanent("Legolas, Counter of Kills")!!

            game.castSpell(1, "Test Scry Spell").error shouldBe null
            game.resolveStack()
            game.answerScry()
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()
            withClue("the first accepted offer untapped Legolas") {
                game.state.getEntity(legolas)!!.get<TappedComponent>().shouldBeNull()
            }

            // Re-tap it so the intervening-if would be satisfied; the spent budget must still
            // stop the ability from triggering for the rest of the turn.
            game.state = game.state.updateEntity(legolas) { it.with(TappedComponent) }

            game.castSpell(1, "Test Scry Spell").error shouldBe null
            game.resolveStack()
            game.answerScry()
            game.resolveStack()
            withClue("the untap was taken this turn, so no further scry triggers the ability") {
                game.hasPendingDecision() shouldBe false
            }
            withClue("and Legolas stays tapped") {
                game.state.getEntity(legolas)!!.get<TappedComponent>().shouldNotBeNull()
            }
        }

        test("an untapped Legolas is not offered the untap at all (intervening-if)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Legolas, Counter of Kills")
                .withCardInHand(1, "Test Scry Spell")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Scry Spell").error shouldBe null
            game.resolveStack()
            game.answerScry()
            game.resolveStack()

            withClue("Legolas is untapped, so the trigger condition fails — no prompt") {
                game.hasPendingDecision() shouldBe false
            }
        }
    }
}
