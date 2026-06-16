package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Kellan Joins Up (OTJ #212) — {G}{W}{U} Legendary Enchantment.
 *
 *   "When Kellan Joins Up enters, you may exile a nonland card with mana value 3 or less from
 *    your hand. If you do, it becomes plotted.
 *    Whenever a legendary creature you control enters, put a +1/+1 counter on each creature you
 *    control."
 *
 * Verifies the ETB plots a chosen MV≤3 nonland card from hand, and the legendary-enters trigger
 * distributes a +1/+1 counter over every creature you control.
 */
class KellanJoinsUpScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Legend",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype.HUMAN),
                supertypes = setOf(Supertype.LEGENDARY),
                power = 2,
                toughness = 2,
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Grunt",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype.HUMAN),
                power = 1,
                toughness = 1,
            )
        )

        context("Kellan Joins Up") {

            test("ETB exiles and plots a chosen MV<=3 nonland card from hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kellan Joins Up")
                    .withCardInHand(1, "Grizzly Bears") // {1}{G}, MV 2, nonland
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kellan Joins Up").error shouldBe null
                game.resolveStack()

                // "You may exile" — choose the Grizzly Bears.
                val bears = game.findCardsInHand(1, "Grizzly Bears").first()
                game.selectCards(listOf(bears))
                game.resolveStack()

                withClue("Grizzly Bears leaves hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                }
                withClue("Grizzly Bears is exiled (plotted)") {
                    game.state.getExile(game.player1Id)
                        .mapNotNull { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name }
                        .contains("Grizzly Bears") shouldBe true
                }
            }

            test("legendary creature entering puts a +1/+1 counter on each creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Legend")
                    .withCardOnBattlefield(1, "Kellan Joins Up")
                    .withCardOnBattlefield(1, "Test Grunt")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grunt = game.findPermanent("Test Grunt")!!

                game.castSpell(1, "Test Legend").error shouldBe null
                game.resolveStack()

                val legend = game.findPermanent("Test Legend")!!

                fun counters(id: com.wingedsheep.sdk.model.EntityId) =
                    game.state.getEntity(id)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

                withClue("Each creature you control gets a +1/+1 counter") {
                    counters(grunt) shouldBe 1
                    counters(legend) shouldBe 1
                }
            }
        }
    }
}
