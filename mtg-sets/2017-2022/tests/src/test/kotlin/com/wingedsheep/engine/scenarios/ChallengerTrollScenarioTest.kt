package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Challenger Troll (WAR #157): "Each creature you control with power 4 or greater can't be
 * blocked by more than one creature."
 *
 * The static lives on the Troll but restricts *other* attackers, so these pin the group-scoped
 * read in blocker validation: the Troll itself and another power-4 attacker are covered, a
 * smaller attacker and an opponent's creature are not.
 */
class ChallengerTrollScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature("Blocker A", ManaCost.parse("{1}"), emptySet(), power = 1, toughness = 1)
        )
        cardRegistry.register(
            CardDefinition.creature("Blocker B", ManaCost.parse("{1}"), emptySet(), power = 1, toughness = 1)
        )
        cardRegistry.register(
            CardDefinition.creature("Four Power Brute", ManaCost.parse("{3}"), emptySet(), power = 4, toughness = 4)
        )

        fun board(attacker: String) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Challenger Troll", summoningSickness = false)
            .withCardOnBattlefield(1, attacker, summoningSickness = false)
            .withCardOnBattlefield(2, "Blocker A")
            .withCardOnBattlefield(2, "Blocker B")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("Challenger Troll itself can't be blocked by two creatures, but can by one") {
            val game = board("Grizzly Bears")
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Challenger Troll" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("double block on the Troll is illegal") {
                game.declareBlockers(
                    mapOf("Blocker A" to listOf("Challenger Troll"), "Blocker B" to listOf("Challenger Troll"))
                ).error shouldNotBe null
            }
            withClue("a single block on the Troll is legal") {
                game.declareBlockers(mapOf("Blocker A" to listOf("Challenger Troll"))).error shouldBe null
            }
        }

        test("another creature you control with power 4 can't be blocked by two creatures") {
            val game = board("Four Power Brute")
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Four Power Brute" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(
                mapOf("Blocker A" to listOf("Four Power Brute"), "Blocker B" to listOf("Four Power Brute"))
            ).error shouldNotBe null
        }

        test("a creature you control with power less than 4 can still be double-blocked") {
            val game = board("Grizzly Bears")
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val result = game.declareBlockers(
                mapOf("Blocker A" to listOf("Grizzly Bears"), "Blocker B" to listOf("Grizzly Bears"))
            )
            withClue("Grizzly Bears (2/2) isn't covered: ${result.error}") { result.error shouldBe null }
        }

        test("an opponent's power-4 creature isn't covered") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Challenger Troll")
                .withCardOnBattlefield(1, "Blocker A")
                .withCardOnBattlefield(1, "Blocker B")
                .withCardOnBattlefield(2, "Four Power Brute", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Four Power Brute" to 1)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val result = game.declareBlockers(
                mapOf("Blocker A" to listOf("Four Power Brute"), "Blocker B" to listOf("Four Power Brute"))
            )
            withClue("the Troll only covers its controller's creatures: ${result.error}") { result.error shouldBe null }
        }
    }
}
