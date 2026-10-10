package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Culling Ritual (STX #172) — {2}{B}{G} Sorcery
 *
 * Destroy each nonland permanent with mana value 2 or less. Add {B} or {G} for each permanent
 * destroyed this way.
 *
 * The mana amount is a count read off the stored destroyed-collection, then split between two
 * colors — the composition the snapshot can't prove.
 */
class CullingRitualScenarioTest : ScenarioTestBase() {

    init {
        context("Culling Ritual") {

            test("destroys cheap nonland permanents and adds one B-or-G mana per permanent destroyed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Culling Ritual")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Culling Ritual").error shouldBe null
                game.resolveStack()

                withClue("Three permanents were destroyed, so the split prompt is pending") {
                    game.hasPendingDecision() shouldBe true
                }
                // Two allowed colors: the prompt asks how much of the first ({B}); the rest is {G}.
                game.submitDecision(NumberChosenResponse(game.getPendingDecision()!!.id, 1))

                withClue("Mana value 1, 2 and 0 permanents are destroyed") {
                    game.isInGraveyard(1, "Llanowar Elves") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Ornithopter") shouldBe true
                }
                withClue("A mana value 4 creature survives") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("Lands are spared even though their mana value is 0") {
                    game.findAllPermanents("Swamp").size shouldBe 2
                }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("One {B} and two {G} — three mana for three destroyed") {
                    pool?.black shouldBe 1
                    pool?.green shouldBe 2
                }
            }

            test("adds no mana when nothing is destroyed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Culling Ritual")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Culling Ritual").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isOnBattlefield("Hill Giant") shouldBe true
                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                pool?.black shouldBe 0
                pool?.green shouldBe 0
            }
        }
    }
}
