package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Expand the Sphere (ONE #168) — {3}{G} Sorcery.
 *
 *   Look at the top six cards of your library. Put up to two land cards from among them onto the
 *   battlefield tapped and the rest on the bottom of your library in a random order. If you put
 *   fewer than two lands onto the battlefield this way, proliferate a number of times equal to
 *   the difference.
 */
class ExpandTheSphereScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun name(game: TestGame, id: EntityId): String? = game.state.getEntity(id)?.get<CardComponent>()?.name

    private fun setup(): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Expand the Sphere")
            .withCardOnBattlefield(1, "Hill Giant")
            .withLandsOnBattlefield(1, "Forest", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        // Top six: two Swamps and four Grizzly Bears; one more card below them.
        builder.withCardInLibrary(1, "Swamp")
        builder.withCardInLibrary(1, "Grizzly Bears")
        builder.withCardInLibrary(1, "Swamp")
        repeat(3) { builder.withCardInLibrary(1, "Grizzly Bears") }
        builder.withCardInLibrary(1, "Island")
        builder.withCardInLibrary(2, "Island")
        val game = builder.build()
        val giant = game.findPermanent("Hill Giant")!!
        game.state = game.state.updateEntity(giant) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        }
        return game
    }

    /** Casts the spell; answers the land pick with [landsToPick] Swamps, then every proliferate with the Giant. */
    private fun castAndResolve(game: TestGame, landsToPick: Int): Int {
        val giant = game.findPermanent("Hill Giant")!!
        val swamps = game.state.getLibrary(game.player1Id).filter { name(game, it) == "Swamp" }.take(landsToPick)
        game.castSpell(1, "Expand the Sphere").error shouldBe null
        var landPicked = false
        var proliferates = 0
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
            if (game.hasPendingDecision()) {
                if (!landPicked) {
                    game.selectCards(swamps)
                    landPicked = true
                } else {
                    game.selectCards(listOf(giant))
                    proliferates++
                }
            } else game.resolveStack()
        }
        return proliferates
    }

    private fun battlefieldSwamps(game: TestGame): List<EntityId> =
        game.state.getBattlefield().filter { name(game, it) == "Swamp" }

    init {
        test("two lands onto the battlefield tapped, no proliferate") {
            val game = setup()
            val giant = game.findPermanent("Hill Giant")!!
            castAndResolve(game, 2) shouldBe 0
            val swamps = battlefieldSwamps(game)
            swamps.size shouldBe 2
            swamps.forEach { game.state.getEntity(it)?.has<TappedComponent>() shouldBe true }
            counters(game, giant) shouldBe 1
            val library = game.state.getLibrary(game.player1Id)
            withClue("Island stays on top; the four Bears go to the bottom") {
                name(game, library.first()) shouldBe "Island"
                library.drop(1).all { name(game, it) == "Grizzly Bears" } shouldBe true
                library.size shouldBe 5
            }
        }

        test("one land onto the battlefield proliferates once") {
            val game = setup()
            val giant = game.findPermanent("Hill Giant")!!
            castAndResolve(game, 1) shouldBe 1
            battlefieldSwamps(game).size shouldBe 1
            counters(game, giant) shouldBe 2
            game.state.getLibrary(game.player1Id).size shouldBe 6
        }

        test("no lands onto the battlefield proliferates twice") {
            val game = setup()
            val giant = game.findPermanent("Hill Giant")!!
            castAndResolve(game, 0) shouldBe 2
            battlefieldSwamps(game).size shouldBe 0
            counters(game, giant) shouldBe 3
            game.isInGraveyard(1, "Expand the Sphere") shouldBe true
        }
    }
}
