package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Orvar, the All-Form (KHM #70).
 *
 * Pins the cast trigger's "other permanents you control" gate (Orvar itself and opponents'
 * permanents don't count), its resolution-time recheck (a captured target that has left the
 * battlefield is ignored), and the discard trigger firing only for an opponent's spell.
 */
class OrvarTheAllFormScenarioTest : ScenarioTestBase() {

    init {
        test("an instant targeting another permanent you control creates a token copy of it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Orvar, the All-Form")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Giant Growth", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            val bears = game.findPermanents("Grizzly Bears")
            bears.size shouldBe 2
            bears.all { game.state.projectedState.getController(it) == game.player1Id } shouldBe true
        }

        test("targeting Orvar itself does not trigger") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Orvar, the All-Form")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Giant Growth", game.findPermanent("Orvar, the All-Form")!!).error shouldBe null
            game.resolveStack()

            game.findPermanents("Orvar, the All-Form").size shouldBe 1
        }

        test("targeting an opponent's permanent does not trigger") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Orvar, the All-Form")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Giant Growth", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.findPermanents("Grizzly Bears").size shouldBe 1
        }

        test("a target that left the battlefield before resolution is not copied") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Orvar, the All-Form")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val original = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", original).error shouldBe null
            // In response, Bolt the Bears. Bolt's own Orvar trigger resolves while the Bears are
            // still there (one token); then the Bears die, so Giant Growth's trigger finds no
            // captured target left on the battlefield and makes nothing.
            game.castSpell(1, "Lightning Bolt", original).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.findPermanents("Grizzly Bears").size shouldBe 1
        }

        test("an opponent's discard spell lets you copy target permanent") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Orvar, the All-Form")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Mind Rot")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(2, "Mind Rot", 1).error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Orvar, the All-Form") shouldBe true
            val bears = game.findPermanents("Grizzly Bears")
            bears.size shouldBe 2
            bears.count { game.state.projectedState.getController(it) == game.player1Id } shouldBe 1
        }

        test("discarding Orvar to your own discard spell does not trigger") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Orvar, the All-Form")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Mind Rot")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Mind Rot", 1).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Orvar, the All-Form") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.findPermanents("Grizzly Bears").size shouldBe 1
        }
    }
}
