package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Omnath, Locus of Creation (ZNR #237) — the landfall ability counts its own resolutions this turn.
 *
 * That tally belongs to the permanent: an Omnath that is blinked returns as a new object
 * (CR 400.7) whose next landfall is its *first*, not its second.
 */
class OmnathLocusOfCreationScenarioTest : ScenarioTestBase() {

    init {
        context("Omnath, Locus of Creation") {

            test("the second landfall resolution this turn adds RGWU instead of gaining life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Omnath, Locus of Creation")
                    .withCardOnBattlefield(1, "Exploration")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forests = game.findCardsInHand(1, "Forest")
                game.execute(PlayLand(game.player1Id, forests[0])).error.shouldBeNull()
                game.resolveStack()
                withClue("first resolution gains 4") { game.getLifeTotal(1) shouldBe 24 }

                game.execute(PlayLand(game.player1Id, forests[1])).error.shouldBeNull()
                game.resolveStack()
                withClue("second resolution gains nothing") { game.getLifeTotal(1) shouldBe 24 }
                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("second resolution adds {R}{G}{W}{U}") {
                    pool?.red shouldBe 1
                    pool?.green shouldBe 1
                    pool?.white shouldBe 1
                    pool?.blue shouldBe 1
                }
            }

            test("a blinked Omnath starts counting afresh") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Omnath, Locus of Creation")
                    .withCardOnBattlefield(1, "Exploration")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Cloudshift")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forests = game.findCardsInHand(1, "Forest")
                game.execute(PlayLand(game.player1Id, forests[0])).error.shouldBeNull()
                game.resolveStack()
                withClue("first resolution gains 4") { game.getLifeTotal(1) shouldBe 24 }

                val omnath = game.findPermanent("Omnath, Locus of Creation")!!
                game.castSpell(1, "Cloudshift", omnath).error.shouldBeNull()
                game.resolveStack()

                game.execute(PlayLand(game.player1Id, forests[1])).error.shouldBeNull()
                game.resolveStack()
                withClue("the returned Omnath's first resolution gains 4 again") {
                    game.getLifeTotal(1) shouldBe 28
                }
            }
        }
    }
}
