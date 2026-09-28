package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.core.TargetsResponse
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Invasion of Alara // Awaken the Maelstrom.
 *
 * Front: exile from the top until two nonland cards with mana value 4 or less are exiled; you may
 * cast one free; put one of them into your hand; the misses go to the bottom in a random order —
 * and, per the ruling, a hit neither cast nor handed stays in exile.
 * Back: target player draws two, you may put an artifact from hand onto the battlefield, copy a
 * permanent you control, distribute three +1/+1 counters among one to three creatures you
 * control, destroy target permanent an opponent controls.
 */
class InvasionOfAlaraScenarioTest : ScenarioTestBase() {

    private fun TestGame.library(player: Int): List<String> {
        val id = if (player == 1) player1Id else player2Id
        return state.getZone(ZoneKey(id, Zone.LIBRARY)).map { state.getEntity(it)!!.get<CardComponent>()!!.name }
    }

    /** Library, top-down: a land, a 6-drop (miss), Bears (hit), a land, Hill Giant (hit), Forest. */
    private fun frontGame(vararg library: String = arrayOf(
        "Island", "Craw Wurm", "Grizzly Bears", "Mountain", "Hill Giant", "Forest",
    )) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Invasion of Alara")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .apply { library.forEach { withCardInLibrary(1, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castInvasionAndTrigger() {
        castSpell(1, "Invasion of Alara").error shouldBe null
        resolveStack()
    }

    private fun TestGame.exiledCard(name: String): EntityId =
        state.getZone(ZoneKey(player1Id, Zone.EXILE)).first {
            state.getEntity(it)!!.get<CardComponent>()!!.name == name
        }

    init {
        context("front face — Invasion of Alara") {
            test("cast one hit free, the other goes to hand, the misses go to the bottom") {
                val game = frontGame()
                game.castInvasionAndTrigger()

                // The capped free-cast loop offers the hits; take Grizzly Bears.
                game.selectCards(listOf(game.exiledCard("Grizzly Bears"))).error shouldBe null
                // One hit left in exile: "put one of them into your hand" has a single candidate.
                if (game.hasPendingDecision()) game.selectCards(listOf(game.exiledCard("Hill Giant")))
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
                withClue("Forest was never exiled; the three misses went under it") {
                    val lib = game.library(1)
                    lib.first() shouldBe "Forest"
                    lib.drop(1) shouldContainExactlyInAnyOrder listOf("Island", "Craw Wurm", "Mountain")
                }
                game.isInExile(1, "Grizzly Bears") shouldBe false
            }

            test("declining the cast: one hit to hand, the other stays in exile") {
                val game = frontGame()
                game.castInvasionAndTrigger()

                game.skipSelection().error shouldBe null
                game.selectCards(listOf(game.exiledCard("Grizzly Bears"))).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                withClue("ruling: the hit you don't put into your hand remains in exile") {
                    game.isInExile(1, "Hill Giant") shouldBe true
                }
                game.library(1).drop(1) shouldContainExactlyInAnyOrder listOf("Island", "Craw Wurm", "Mountain")
            }

            test("only one hit before the library runs out: declining puts it into your hand") {
                val game = frontGame("Island", "Grizzly Bears", "Craw Wurm")
                game.castInvasionAndTrigger()

                game.skipSelection().error shouldBe null
                if (game.hasPendingDecision()) game.selectCards(listOf(game.exiledCard("Grizzly Bears")))
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.library(1) shouldContainExactlyInAnyOrder listOf("Island", "Craw Wurm")
            }
        }

        context("back face — Awaken the Maelstrom") {
            test("draw two, drop an artifact, copy it, distribute three counters, destroy") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Invasion of Alara")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.checkStateBasedActions()
                val siege = game.findPermanent("Invasion of Alara")!!
                game.state = game.state.updateEntity(siege) {
                    it.with(CountersComponent(mapOf(CounterType.DEFENSE to 3)))
                }

                game.castSpell(1, "Lightning Bolt", siege).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                // Targets: the drawing player, then the permanent to destroy.
                val giant = game.findPermanent("Hill Giant")!!
                val decision = game.getPendingDecision()!!
                game.submitDecision(
                    TargetsResponse(decision.id, mapOf(0 to listOf(game.player1Id), 1 to listOf(giant)))
                ).error shouldBe null
                val handBefore = game.handSize(1)
                game.resolveStack()

                // May put an artifact from hand: Ornithopter.
                game.selectCards(game.findCardsInHand(1, "Ornithopter")).error shouldBe null
                // Copy a permanent you control: the Ornithopter just put onto the battlefield.
                val thopter = game.findPermanent("Ornithopter")!!
                game.selectCards(listOf(thopter)).error shouldBe null
                // Distribute three +1/+1 counters: two on Bears, one on the original Ornithopter.
                val bears = game.findPermanent("Grizzly Bears")!!
                game.submitDistribution(mapOf(bears to 2, thopter to 1)).error shouldBe null
                game.resolveStack()

                withClue("drew two, then the Ornithopter left the hand") { game.handSize(1) shouldBe handBefore + 1 }
                game.findPermanents("Ornithopter").size shouldBe 2
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getPower(thopter) shouldBe 1
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(1, "Invasion of Alara") shouldBe true
            }
        }
    }
}
