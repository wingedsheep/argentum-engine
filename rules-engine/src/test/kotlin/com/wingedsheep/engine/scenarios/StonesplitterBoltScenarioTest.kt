package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Scenario tests for Stonesplitter Bolt. */
class StonesplitterBoltScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun auraOn(game: TestGame, auraName: String, host: EntityId): EntityId? =
        game.findPermanents(auraName).firstOrNull { aura ->
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId == host
        }

    init {
        context("Stonesplitter Bolt — X damage, twice X if bargained") {
            test("unbargained deals exactly X") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stonesplitter Bolt")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                // X = 1 into a 2/2: survives. Were the amount doubled without bargain, it would die.
                game.castXSpell(1, "Stonesplitter Bolt", xValue = 1, targetId = bears)
                    .error shouldBe null
                game.resolveStack()

                withClue("1 damage does not kill a 2/2") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("bargained deals twice X — the same X that survived unbargained now kills") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stonesplitter Bolt")
                    .withCardOnBattlefield(1, "A Tale for the Ages")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                // Sacrifice the enchantment to bargain; X = 1 becomes 2 damage, lethal to the 2/2.
                game.castSpellBargained(
                    1,
                    "Stonesplitter Bolt",
                    sacrificeName = "A Tale for the Ages",
                    targetId = bears,
                    xValue = 1,
                ).error shouldBe null
                game.resolveStack()

                withClue("the enchantment paid the bargain cost") {
                    game.isInGraveyard(1, "A Tale for the Ages") shouldBe true
                }
                withClue("twice X = 2 damage kills the 2/2") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("X = 0 bargained is still 0 damage — 'twice X', not a flat doubling") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stonesplitter Bolt")
                    .withCardOnBattlefield(1, "A Tale for the Ages")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions").shouldNotBeNull()

                game.castSpellBargained(
                    1,
                    "Stonesplitter Bolt",
                    sacrificeName = "A Tale for the Ages",
                    targetId = lions,
                    xValue = 0,
                ).error shouldBe null
                game.resolveStack()

                withClue("twice 0 is 0 — the 2/1 lives") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }
            }
        }
    }
}
