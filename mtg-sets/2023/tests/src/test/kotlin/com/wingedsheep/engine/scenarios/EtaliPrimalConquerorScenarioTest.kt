package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Etali, Primal Conqueror // Etali, Primal Sickness (MOM #137).
 *
 * The ETB exiles from the top of *each* library until a nonland is exiled from it (lands stay in
 * exile), then lets the controller cast any number of those nonlands for free (G36).
 */
class EtaliPrimalConquerorScenarioTest : ScenarioTestBase() {

    private fun TestGame.exileNames(player: Int): List<String> {
        val id = if (player == 1) player1Id else player2Id
        return state.getZone(ZoneKey(id, Zone.EXILE)).map { state.getEntity(it)!!.get<CardComponent>()!!.name }
    }

    private fun TestGame.exiled(name: String): EntityId =
        listOf(player1Id, player2Id).flatMap { state.getZone(ZoneKey(it, Zone.EXILE)) }
            .first { state.getEntity(it)!!.get<CardComponent>()!!.name == name }

    private fun game(p1Library: List<String>, p2Library: List<String>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Etali, Primal Conqueror")
        .withLandsOnBattlefield(1, "Mountain", 7)
        .apply { p1Library.forEach { withCardInLibrary(1, it) } }
        .apply { p2Library.forEach { withCardInLibrary(2, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("each library is walked to its own nonland; cast both free") {
            val game = game(
                listOf("Island", "Grizzly Bears", "Forest"),
                listOf("Mountain", "Swamp", "Hill Giant", "Plains"),
            )
            game.castSpell(1, "Etali, Primal Conqueror").error shouldBe null
            game.resolveStack()

            // "Any number" is an accumulating loop: one pick per prompt, then the next.
            game.selectCards(listOf(game.exiled("Grizzly Bears"))).error shouldBe null
            game.selectCards(listOf(game.exiled("Hill Giant"))).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            withClue("lands revealed along the way stay exiled; cards below the nonland are untouched") {
                game.exileNames(1) shouldContainExactlyInAnyOrder listOf("Island")
                game.exileNames(2) shouldContainExactlyInAnyOrder listOf("Mountain", "Swamp")
            }
        }

        test("casting any subset: decline one, it stays exiled") {
            val game = game(listOf("Grizzly Bears", "Forest"), listOf("Hill Giant", "Plains"))
            game.castSpell(1, "Etali, Primal Conqueror").error shouldBe null
            game.resolveStack()

            game.selectCards(listOf(game.exiled("Hill Giant"))).error shouldBe null
            if (game.hasPendingDecision()) game.skipSelection().error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.exileNames(1) shouldContainExactlyInAnyOrder listOf("Grizzly Bears")
        }

        test("an empty library contributes nothing and doesn't break the walk") {
            val game = game(listOf("Grizzly Bears"), emptyList())
            game.castSpell(1, "Etali, Primal Conqueror").error shouldBe null
            game.resolveStack()

            game.selectCards(listOf(game.exiled("Grizzly Bears"))).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
