package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Marang River Prowler (FRF #40) — {2}{U} Creature — Human Rogue 2/1.
 *
 *   This creature can't block and can't be blocked.
 *   You may cast this card from your graveyard as long as you control a black or green permanent.
 *
 * Pins the conditional graveyard-cast permission: a black or a green permanent you control enables
 * it, while no such permanent — or only an opponent's green creature — leaves it unavailable.
 */
class MarangRiverProwlerScenarioTest : ScenarioTestBase() {

    init {
        context("Marang River Prowler") {

            test("castable from graveyard while you control a black permanent; resolves onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Marang River Prowler")
                    .withCardOnBattlefield(1, "Scathe Zombies")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellFromGraveyard(1, "Marang River Prowler")
                withClue("cast from graveyard should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.findPermanent("Marang River Prowler") shouldNotBe null
                game.isInGraveyard(1, "Marang River Prowler") shouldBe false
            }

            test("castable from graveyard while you control a green permanent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Marang River Prowler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellFromGraveyard(1, "Marang River Prowler")
                withClue("cast from graveyard should succeed: ${cast.error}") { cast.error shouldBe null }
            }

            test("not castable from graveyard without a black or green permanent of your own") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Marang River Prowler")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellFromGraveyard(1, "Marang River Prowler")
                withClue("a white creature, colourless lands and an opponent's green creature don't count") {
                    cast.error shouldNotBe null
                }
                game.isInGraveyard(1, "Marang River Prowler") shouldBe true
            }
        }
    }
}
