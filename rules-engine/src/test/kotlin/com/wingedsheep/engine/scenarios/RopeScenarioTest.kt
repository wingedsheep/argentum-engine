package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.Rope
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Rope (MKM #173).
 *
 * "Equipped creature gets +1/+2, has reach, and can't be blocked by more than one creature."
 *
 * The blocking restriction is the reason this test exists. `BlockPhaseManager` reads the printed
 * [com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan] static off the *attacker's own* card
 * definition and only when its filter scope is `Self`, so the obvious spelling — that static
 * scoped to `Filters.EquippedCreature` on the Equipment — would compile, snapshot cleanly, and
 * silently never restrict anything. Rope instead grants the
 * [com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE] keyword through the
 * layer system, which the same validator honors via its projected-keyword branch. These tests pin
 * that down: a double block is rejected, a single block is legal, and the restriction leaves with
 * the Equipment.
 */
class RopeScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(Rope)
        cardRegistry.register(
            CardDefinition.creature("Rope Blocker A", ManaCost.parse("{1}"), emptySet(), power = 1, toughness = 1)
        )
        cardRegistry.register(
            CardDefinition.creature("Rope Blocker B", ManaCost.parse("{1}"), emptySet(), power = 1, toughness = 1)
        )

        test("equipped creature gets +1/+2 and has reach") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                .withCardAttachedTo(1, "Rope", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = StateProjector().project(game.state)

            withClue("Grizzly Bears should be 3/4 with Rope attached") {
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 4
            }
            withClue("Grizzly Bears should have reach from Rope") {
                projected.hasKeyword(bears, Keyword.REACH) shouldBe true
            }
        }

        test("the equipped attacker can't be blocked by two creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Rope", "Grizzly Bears")
                .withCardOnBattlefield(2, "Rope Blocker A")
                .withCardOnBattlefield(2, "Rope Blocker B")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val doubleBlock = game.declareBlockers(
                mapOf(
                    "Rope Blocker A" to listOf("Grizzly Bears"),
                    "Rope Blocker B" to listOf("Grizzly Bears"),
                )
            )
            withClue("Rope caps the equipped creature at one blocker") {
                doubleBlock.error shouldNotBe null
            }
        }

        test("a single creature can still block the equipped attacker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Rope", "Grizzly Bears")
                .withCardOnBattlefield(2, "Rope Blocker A")
                .withCardOnBattlefield(2, "Rope Blocker B")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val singleBlock = game.declareBlockers(mapOf("Rope Blocker A" to listOf("Grizzly Bears")))
            withClue("One blocker is legal: ${singleBlock.error}") {
                singleBlock.error shouldBe null
            }
        }

        test("an unequipped creature is not restricted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Rope")
                .withCardOnBattlefield(2, "Rope Blocker A")
                .withCardOnBattlefield(2, "Rope Blocker B")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val doubleBlock = game.declareBlockers(
                mapOf(
                    "Rope Blocker A" to listOf("Grizzly Bears"),
                    "Rope Blocker B" to listOf("Grizzly Bears"),
                )
            )
            withClue("With Rope sitting unattached, two creatures may gang-block: ${doubleBlock.error}") {
                doubleBlock.error shouldBe null
            }
        }
    }
}
