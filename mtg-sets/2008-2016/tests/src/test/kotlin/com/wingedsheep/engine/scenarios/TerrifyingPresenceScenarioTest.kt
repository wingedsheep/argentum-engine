package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Terrifying Presence (AVR #196) — {1}{G} Instant.
 *
 *   Prevent all combat damage that would be dealt by creatures other than target creature this turn.
 *
 * The target is pinned as the spell resolves (`ResolutionFilterBaker` bakes `otherThan(target)` to
 * its id), so in the turn's combat it alone deals combat damage — attacking or blocking — while
 * every other creature, on either side, deals none. The player badge names the excluded creature.
 */
class TerrifyingPresenceScenarioTest : ScenarioTestBase() {

    init {
        test("only the target creature deals combat damage this turn; every other creature's is prevented") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Terrifying Presence")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            game.execute(PassPriority(game.player2Id))
            game.castSpell(1, "Terrifying Presence", courser).error shouldBe null
            game.resolveStack()

            withClue("the shield's badge names the creature it leaves out") {
                val badges = stateTransformer.transform(game.state, game.player1Id).players
                    .flatMap { it.activeEffects }
                    .filter { it.effectId.startsWith("prevent_combat_damage_from_") }
                badges.isNotEmpty() shouldBe true
                badges.all { "other than Centaur Courser" in it.description.orEmpty() } shouldBe true
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 1, "Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the unblocked target Centaur Courser dealt its 3 combat damage") {
                game.getLifeTotal(1) shouldBe 17
            }
            withClue("the Hill Giant's damage to the blocked Grizzly Bears was prevented, and the Bears' to it") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }

        test("the target blocking deals its damage; the other attacker's is prevented") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Terrifying Presence")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            game.execute(PassPriority(game.player2Id))
            game.castSpell(1, "Terrifying Presence", courser).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the blocking Centaur Courser killed the Grizzly Bears and took none back") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Centaur Courser") shouldBe true
            }
            withClue("the unblocked Hill Giant's combat damage to the player was prevented") {
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
