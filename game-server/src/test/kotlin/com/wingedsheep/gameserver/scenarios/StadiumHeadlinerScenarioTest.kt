package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Stadium Headliner (TDM #122) — {R} Goblin Warrior, 1/1, Mobilize 1.
 *
 * "{1}{R}, Sacrifice this creature: It deals damage equal to the number of creatures you control
 *  to target creature."
 *
 * Stadium Headliner is sacrificed as a cost, so it is no longer a creature you control when the
 * ability resolves. With Stadium Headliner plus two other creatures, the count at resolution is 2,
 * so it deals 2 damage to the target — enough to kill a 2-toughness creature.
 */
class StadiumHeadlinerScenarioTest : ScenarioTestBase() {

    private val sacAbilityId =
        cardRegistry.getCard("Stadium Headliner")!!.activatedAbilities.first().id

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Goblin A",
                manaCost = ManaCost.parse("{R}"),
                subtypes = setOf(Subtype("Goblin")),
                power = 1,
                toughness = 1
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Goblin B",
                manaCost = ManaCost.parse("{R}"),
                subtypes = setOf(Subtype("Goblin")),
                power = 1,
                toughness = 1
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Enemy Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
        )

        context("Stadium Headliner sacrifice ability") {

            test("deals damage equal to creatures you control (after the sacrifice) to a target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stadium Headliner", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test Goblin A", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test Goblin B", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Enemy Bear", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val headliner = game.findPermanent("Stadium Headliner")!!
                val bear = game.findPermanent("Enemy Bear")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = headliner,
                        abilityId = sacAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bear))
                    )
                )
                withClue("Activating the sacrifice ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Stadium Headliner was sacrificed as a cost and left the battlefield") {
                    game.isOnBattlefield("Stadium Headliner") shouldBe false
                }
                withClue("Two creatures remained at resolution, so 2 damage kills the 2/2 Enemy Bear") {
                    game.isOnBattlefield("Enemy Bear") shouldBe false
                }
            }

            test("with more creatures the damage scales up") {
                // Stadium Headliner + three others = 4 creatures; after sacrifice, 3 remain → 3 damage.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Stadium Headliner", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test Goblin A", summoningSickness = false)
                    .withCardOnBattlefield(1, "Test Goblin B", summoningSickness = false)
                    .withCardOnBattlefield(1, "Stadium Headliner", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Enemy Bear", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val headliners = game.findPermanents("Stadium Headliner")
                val bear = game.findPermanent("Enemy Bear")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = headliners.first(),
                        abilityId = sacAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bear))
                    )
                )
                withClue("Activating the sacrifice ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Three creatures remain (other Headliner + two Goblins) → 3 damage to the 2/2 bear.
                withClue("Enemy Bear (2/2) should be dead from 3 damage") {
                    game.isOnBattlefield("Enemy Bear") shouldBe false
                }
            }
        }
    }
}
