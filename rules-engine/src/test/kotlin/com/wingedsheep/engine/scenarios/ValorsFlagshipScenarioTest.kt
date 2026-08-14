package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Valor's Flagship — a 7/7 Vehicle with flying, first strike, lifelink and Crew 3, plus
 * Cycling {X}{2}{W} and "When you cycle this card, create X 1/1 colorless Pilot creature tokens
 * with 'This token saddles Mounts and crews Vehicles as though its power were 2 greater.'"
 *
 * The X announced for the cycling cost (CR 107.3a) is what the trigger's token count reads, so
 * these cases pin the count against the announcement — including X = 0, which legally creates
 * nothing. The token's crew rider matters as much as the count: a lone 1/1 Pilot could never crew
 * a Crew 3 Vehicle on its printed power, but crews as though its power were 3.
 */
class ValorsFlagshipScenarioTest : ScenarioTestBase() {

    init {
        test("cycling for X=3 creates three Pilot tokens") {
            val game = flagshipGame(plains = 6)

            val cycle = game.cycleCard(1, "Valor's Flagship", xValue = 3)
            withClue("Cycling for X=3 should succeed: ${cycle.error}") { cycle.error shouldBe null }
            game.resolveStack()

            withClue("X = 3 tokens") { game.findPermanents("Pilot Token").size shouldBe 3 }
            withClue("The Flagship itself was discarded to pay the cycling cost") {
                game.isInGraveyard(1, "Valor's Flagship") shouldBe true
            }
        }

        test("X=0 creates no tokens but still cycles and draws") {
            // Three Plains pay {0}{2}{W} exactly — proof the {X} was charged as 0, not skipped.
            val game = flagshipGame(plains = 3)
            val handBefore = game.handSize(1)

            val cycle = game.cycleCard(1, "Valor's Flagship", xValue = 0)
            withClue("Cycling for X=0 should succeed on three Plains: ${cycle.error}") {
                cycle.error shouldBe null
            }
            game.resolveStack()

            withClue("X = 0 tokens") { game.findPermanents("Pilot Token").size shouldBe 0 }
            withClue("Cycling still drew a card, replacing the discarded Flagship") {
                game.handSize(1) shouldBe handBefore
                game.isInGraveyard(1, "Valor's Flagship") shouldBe true
            }
        }

        test("the Pilot tokens carry the crew rider — one alone crews a Crew 3 Vehicle") {
            val game = flagshipGame(plains = 6, extraVehicle = "Detention Chariot")
            val vehicle = game.findPermanent("Detention Chariot")!!

            val cycle = game.cycleCard(1, "Valor's Flagship", xValue = 1)
            withClue("Cycling for X=1 should succeed: ${cycle.error}") { cycle.error shouldBe null }
            game.resolveStack()

            val pilots = game.findPermanents("Pilot Token")
            withClue("X = 1 token") { pilots.size shouldBe 1 }

            val crew = game.execute(CrewVehicle(game.player1Id, vehicle, pilots))
            withClue("Crewing should be paid by the lone Pilot: ${crew.error}") {
                crew.error shouldBe null
            }
            game.resolveStack()
            withClue("A lone 1/1 Pilot crews for 3 thanks to the +2 rider") {
                game.state.projectedState.isCreature(vehicle) shouldBe true
            }
        }

        test("the Flagship on the battlefield is a 7/7 Vehicle with its three keywords") {
            val game = flagshipGame(plains = 6, flagshipInPlay = true)
            val flagship = game.findPermanent("Valor's Flagship")!!
            val projected = game.state.projectedState

            withClue("A Vehicle is not a creature until crewed, but its P/T is printed 7/7") {
                projected.getPower(flagship) shouldBe 7
                projected.getToughness(flagship) shouldBe 7
            }
            withClue("Flying, first strike and lifelink are printed on the Vehicle") {
                projected.hasKeyword(flagship, Keyword.FLYING) shouldBe true
                projected.hasKeyword(flagship, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(flagship, Keyword.LIFELINK) shouldBe true
            }
        }
    }

    /** The Flagship in hand (or in play), and [plains] Plains to cycle it with. */
    private fun flagshipGame(
        plains: Int,
        extraVehicle: String? = null,
        flagshipInPlay: Boolean = false
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withLandsOnBattlefield(1, "Plains", plains)
        if (flagshipInPlay) {
            builder.withCardOnBattlefield(1, "Valor's Flagship", summoningSickness = false)
        } else {
            builder.withCardInHand(1, "Valor's Flagship")
        }
        if (extraVehicle != null) {
            builder.withCardOnBattlefield(1, extraVehicle, summoningSickness = false)
        }
        repeat(8) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }
}
