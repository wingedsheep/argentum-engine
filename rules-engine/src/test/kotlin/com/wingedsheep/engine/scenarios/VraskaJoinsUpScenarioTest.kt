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
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Vraska Joins Up (OTJ #236) — {B}{G} Legendary Enchantment.
 *
 *   "When Vraska Joins Up enters, put a deathtouch counter on each creature you control.
 *    Whenever a legendary creature you control deals combat damage to a player, draw a card."
 *
 * Verifies the ETB distributes a deathtouch counter onto every creature its controller
 * controls (not opposing creatures), and that the ongoing trigger draws a card only when a
 * *legendary* creature you control connects with a player.
 */
class VraskaJoinsUpScenarioTest : ScenarioTestBase() {

    private fun deathtouchCounters(game: TestGame, permanent: EntityId): Int =
        game.state.getEntity(permanent)?.get<CountersComponent>()
            ?.getCount(CounterType.DEATHTOUCH) ?: 0

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
                power = 2,
                toughness = 2,
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Enemy Grunt",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype.HUMAN),
                power = 2,
                toughness = 2,
            )
        )

        context("Vraska Joins Up") {

            test("ETB puts a deathtouch counter on each creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vraska Joins Up")
                    .withCardOnBattlefield(1, "Test Legend")
                    .withCardOnBattlefield(1, "Test Grunt")
                    .withCardOnBattlefield(2, "Enemy Grunt")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myLegend = game.findPermanent("Test Legend")!!
                val myGrunt = game.findPermanent("Test Grunt")!!
                val theirGrunt = game.findPermanent("Enemy Grunt")!!

                game.castSpell(1, "Vraska Joins Up").error shouldBe null
                game.resolveStack()

                withClue("Your creatures gain a deathtouch counter") {
                    deathtouchCounters(game, myLegend) shouldBe 1
                    deathtouchCounters(game, myGrunt) shouldBe 1
                }
                withClue("Opponent's creature is untouched") {
                    deathtouchCounters(game, theirGrunt) shouldBe 0
                }
            }

            test("draws a card when a legendary creature you control deals combat damage to a player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vraska Joins Up")
                    .withCardOnBattlefield(1, "Test Legend")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Test Legend" to 2))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent took 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("Combat damage to player triggers a draw") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("does not draw when a non-legendary creature you control deals combat damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vraska Joins Up")
                    .withCardOnBattlefield(1, "Test Grunt")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Test Grunt" to 2))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent took 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("Non-legendary attacker draws no card") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
