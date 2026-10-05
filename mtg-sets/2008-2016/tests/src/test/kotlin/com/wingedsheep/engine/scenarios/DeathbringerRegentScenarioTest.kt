package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deathbringer Regent (DTK #96) — {5}{B}{B} 5/6 Flying Dragon.
 *
 * "When this creature enters, if you cast it from your hand and there are five or more other
 * creatures on the battlefield, destroy all other creatures."
 *
 * Pins the three edges of the intervening-if: the count excludes the Regent (four others is not
 * enough), five others wipes every other creature while the Regent survives, and entering without
 * being cast (Zombify) never wipes.
 */
class DeathbringerRegentScenarioTest : ScenarioTestBase() {

    init {
        context("Deathbringer Regent") {

            test("cast from hand with exactly four other creatures: no wipe") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Deathbringer Regent")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Deathbringer Regent")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Deathbringer Regent") shouldBe true
                withClue("four other creatures (Regent itself not counted) must not trigger the wipe") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 4
                }
            }

            test("cast from hand with five other creatures: every other creature is destroyed") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Deathbringer Regent")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Deathbringer Regent")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("all five other creatures, on both sides, are destroyed") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 0
                    game.findAllPermanents("Hill Giant").size shouldBe 0
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 2
                    game.findCardsInGraveyard(1, "Hill Giant").size shouldBe 1
                    game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 2
                }
                withClue("the Regent destroys only *other* creatures") {
                    game.isOnBattlefield("Deathbringer Regent") shouldBe true
                }
            }

            test("put onto the battlefield without being cast (Zombify) with five others: no wipe") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInGraveyard(1, "Deathbringer Regent")
                    .withCardInHand(1, "Zombify")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val regent = game.findCardsInGraveyard(1, "Deathbringer Regent").single()
                val cast = game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(regent))
                withClue("Zombify should be castable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Deathbringer Regent") shouldBe true
                withClue("the Regent wasn't cast, so its enters trigger must do nothing") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 4
                    game.findAllPermanents("Hill Giant").size shouldBe 1
                }
            }
        }
    }
}
