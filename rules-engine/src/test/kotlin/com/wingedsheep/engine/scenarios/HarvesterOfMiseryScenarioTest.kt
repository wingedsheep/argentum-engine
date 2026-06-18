package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Harvester of Misery (BIG #9).
 *
 * {3}{B}{B} Creature — Spirit 5/4. Menace.
 *   When this creature enters, other creatures get -2/-2 until end of turn.
 *   {1}{B}, Discard this card: Target creature gets -2/-2 until end of turn.
 */
class HarvesterOfMiseryScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        test("has menace") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Harvester of Misery")
                .build()

            val harvester = game.findPermanent("Harvester of Misery")!!
            projector.project(game.state).hasKeyword(harvester, Keyword.MENACE) shouldBe true
        }

        test("ETB gives other creatures -2/-2 until end of turn, killing small ones but not itself") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Harvester of Misery")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 → dies
                .withCardOnBattlefield(2, "Hill Giant")    // 3/3 → becomes 1/1
                .withCardInLibrary(1, "Swamp")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Harvester of Misery").error shouldBe null
            game.resolveStack()

            withClue("the 2/2 Grizzly Bears is destroyed by -2/-2") {
                game.findPermanent("Grizzly Bears") shouldBe null
            }
            val harvester = game.findPermanent("Harvester of Misery")!!
            val projected = projector.project(game.state)
            withClue("Harvester itself is unaffected (other creatures only) — stays 5/4") {
                projected.getPower(harvester) shouldBe 5
                projected.getToughness(harvester) shouldBe 4
            }
            val giant = game.findPermanent("Hill Giant")
            withClue("Hill Giant survives at 1/1") {
                giant shouldBe game.findPermanent("Hill Giant")
                projected.getPower(giant!!) shouldBe 1
                projected.getToughness(giant) shouldBe 1
            }
        }

        test("discard-from-hand ability gives target creature -2/-2") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Harvester of Misery")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val harvesterCard = game.findCardsInHand(1, "Harvester of Misery").first()
            val abilityId = cardRegistry.getCard("Harvester of Misery")!!.activatedAbilities.first().id

            // {1}{B}, Discard this card: Target creature gets -2/-2.
            val result = game.execute(
                com.wingedsheep.engine.core.ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = harvesterCard,
                    abilityId = abilityId,
                    targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bears))
                )
            )
            result.error shouldBe null
            game.resolveStack()

            withClue("the discard ability resolves: Harvester is in the graveyard") {
                game.isInGraveyard(1, "Harvester of Misery") shouldBe true
            }
            withClue("the targeted 2/2 is destroyed by -2/-2") {
                game.findPermanent("Grizzly Bears") shouldBe null
            }
        }
    }
}
