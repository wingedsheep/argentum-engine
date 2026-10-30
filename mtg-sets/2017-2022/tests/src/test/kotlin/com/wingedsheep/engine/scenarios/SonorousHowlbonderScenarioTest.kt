package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sonorous Howlbonder (IKO #230) — {1}{B/R}{B/R} Creature — Human Warrior 2/2.
 *
 *   Menace
 *   Each creature you control with menace can't be blocked except by three or more creatures.
 *
 * The battlefield-scoped `CantBeBlockedByFewerThan(3)` group clause: Howlbonder and another menace
 * creature (Cavern Whisperer) need three blockers; a creature without menace (Grizzly Bears) is
 * untouched.
 */
class SonorousHowlbonderScenarioTest : ScenarioTestBase() {

    private fun combat() = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Sonorous Howlbonder")
        .withCardOnBattlefield(1, "Cavern Whisperer")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Goblin Guide")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    init {
        test("another creature you control with menace can't be blocked by only two creatures") {
            val game = combat()
            game.declareAttackers(mapOf("Cavern Whisperer" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(
                mapOf(
                    "Savannah Lions" to listOf("Cavern Whisperer"),
                    "Goblin Guide" to listOf("Cavern Whisperer"),
                )
            ).error shouldNotBe null

            game.declareBlockers(
                mapOf(
                    "Savannah Lions" to listOf("Cavern Whisperer"),
                    "Goblin Guide" to listOf("Cavern Whisperer"),
                    "Hill Giant" to listOf("Cavern Whisperer"),
                )
            ).error shouldBe null
        }

        test("Howlbonder itself needs three blockers but may go unblocked") {
            val game = combat()
            game.declareAttackers(mapOf("Sonorous Howlbonder" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(
                mapOf(
                    "Savannah Lions" to listOf("Sonorous Howlbonder"),
                    "Goblin Guide" to listOf("Sonorous Howlbonder"),
                )
            ).error shouldNotBe null
            game.declareNoBlockers().error shouldBe null
        }

        test("a creature without menace can still be blocked by one creature") {
            val game = combat()
            game.declareAttackers(mapOf("Cavern Whisperer" to 2, "Grizzly Bears" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(
                mapOf("Savannah Lions" to listOf("Grizzly Bears")),
            ).error shouldBe null
        }
    }
}
