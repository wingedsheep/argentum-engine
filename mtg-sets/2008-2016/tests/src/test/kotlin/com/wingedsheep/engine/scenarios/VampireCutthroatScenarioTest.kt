package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vampire Cutthroat (EMN #110, {B}, 1/1 Vampire Rogue).
 *
 *   Skulk (This creature can't be blocked by creatures with greater power.)
 *   Lifelink (Damage dealt by this creature also causes you to gain that much life.)
 */
class VampireCutthroatScenarioTest : ScenarioTestBase() {

    private fun attack(blocker: String): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Vampire Cutthroat")
            .withCardOnBattlefield(2, blocker)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Vampire Cutthroat" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    init {
        test("a 2-power creature can't block it, and its unblocked damage gains its controller life") {
            val game = attack("Glory Seeker")
            withClue("skulk: Glory Seeker has greater power") {
                game.declareBlockers(mapOf("Glory Seeker" to listOf("Vampire Cutthroat"))).error shouldNotBe null
            }
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
        }

        test("a 1-power creature can block it") {
            val game = attack("Llanowar Elves")
            game.declareBlockers(mapOf("Llanowar Elves" to listOf("Vampire Cutthroat"))).error shouldBe null
        }
    }
}
