package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Edgar Markov (Commander 2017 #36, reprinted in Innistrad Remastered #234).
 *
 *   Eminence — Whenever you cast another Vampire spell, if Edgar is in the command zone or on the
 *   battlefield, create a 1/1 black Vampire creature token.
 *   First strike, haste
 *   Whenever Edgar attacks, put a +1/+1 counter on each Vampire you control.
 *
 * The eminence ability is the point: its trigger condition functions from the command zone as well
 * as the battlefield (CR 113.6b), and the printed zone clause is also an intervening-"if"
 * (CR 603.4), so it is re-checked as the ability resolves. Both of the card's official rulings get
 * a test.
 */
class EdgarMarkovScenarioTest : ScenarioTestBase() {

    /** +1/+1 counters on [id], or 0 when the permanent has none. */
    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("eminence triggers from the command zone") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Edgar Markov")
                .withCardInHand(1, "Vampire Interloper")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("no token before the Vampire spell is cast") {
                game.findPermanents("Vampire Token").size shouldBe 0
            }

            game.castSpell(1, "Vampire Interloper").error shouldBe null
            game.resolveStack()

            val tokens = game.findPermanents("Vampire Token")
            tokens.size shouldBe 1
            val projected = game.state.projectedState
            projected.getPower(tokens[0]) shouldBe 1
            projected.getToughness(tokens[0]) shouldBe 1
        }

        test("eminence also triggers from the battlefield") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Edgar Markov")
                .withCardInHand(1, "Vampire Interloper")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Vampire Interloper").error shouldBe null
            game.resolveStack()

            game.findPermanents("Vampire Token").size shouldBe 1
        }

        test("eminence stays inert while Edgar is in a zone the ability doesn't function from") {
            // The graveyard is neither the battlefield nor the command zone, so CR 113.6b keeps the
            // trigger from functioning at all.
            val game = scenario()
                .withPlayers()
                .withCardInGraveyard(1, "Edgar Markov")
                .withCardInHand(1, "Vampire Interloper")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Vampire Interloper").error shouldBe null
            game.resolveStack()

            game.findPermanents("Vampire Token").size shouldBe 0
        }

        test("eminence ignores a non-Vampire spell") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Edgar Markov")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.findPermanents("Vampire Token").size shouldBe 0
        }

        test("eminence ignores an opponent's Vampire spell") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "Edgar Markov")
                .withCardInHand(2, "Vampire Interloper")
                .withLandsOnBattlefield(2, "Swamp", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Vampire Interloper").error shouldBe null
            game.resolveStack()

            game.findPermanents("Vampire Token").size shouldBe 0
        }

        test("ruling: Edgar leaving the battlefield before the trigger resolves produces no token") {
            // "If it's on the battlefield or in the command zone when you cast another Vampire spell
            // but leaves that zone before the ability resolves, the ability won't do anything as it
            // resolves." (CR 603.4's second check.)
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Edgar Markov")
                .withCardInHand(1, "Vampire Interloper")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val edgar = game.findPermanent("Edgar Markov")!!

            // Casting the Vampire puts the eminence trigger on the stack above it...
            game.castSpell(1, "Vampire Interloper").error shouldBe null
            // ...and Murder (an instant) goes on top of that, so Edgar dies first.
            game.castSpell(1, "Murder", edgar).error shouldBe null
            game.resolveStack()

            withClue("Edgar is gone") { game.findPermanent("Edgar Markov") shouldBe null }
            withClue("the eminence trigger resolved but did nothing") {
                game.findPermanents("Vampire Token").size shouldBe 0
            }
        }

        test("first strike and haste") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Edgar Markov")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val edgar = game.findPermanent("Edgar Markov")!!
            val projected = game.state.projectedState
            projected.hasKeyword(edgar, Keyword.FIRST_STRIKE) shouldBe true
            projected.hasKeyword(edgar, Keyword.HASTE) shouldBe true
        }

        test("attacking puts a +1/+1 counter on each Vampire you control") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Edgar Markov")
                .withCardOnBattlefield(1, "Vampire Interloper")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Vampire Interloper")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val edgar = game.findPermanent("Edgar Markov")!!
            val myVampire = game.findPermanents("Vampire Interloper")
                .first { game.state.projectedState.getController(it) == game.player1Id }
            val theirVampire = game.findPermanents("Vampire Interloper")
                .first { game.state.projectedState.getController(it) == game.player2Id }
            val bears = game.findPermanent("Grizzly Bears")!!

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Edgar Markov" to 2)).error shouldBe null
            game.resolveStack()

            withClue("Edgar counts himself — he is a Vampire you control") {
                game.plusOneCounters(edgar) shouldBe 1
            }
            game.plusOneCounters(myVampire) shouldBe 1
            withClue("only Vampires") { game.plusOneCounters(bears) shouldBe 0 }
            withClue("only yours") { game.plusOneCounters(theirVampire) shouldBe 0 }
        }

        test("attacking also pumps the tokens eminence made") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Edgar Markov")
                .withCardInHand(1, "Vampire Interloper")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Vampire Interloper").error shouldBe null
            game.resolveStack()
            val token = game.findPermanents("Vampire Token").single()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Edgar Markov" to 2)).error shouldBe null
            game.resolveStack()

            game.plusOneCounters(token) shouldBe 1
            game.state.projectedState.getPower(token) shouldBe 2
        }
    }
}
