package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Drag to the Underworld (THB #89) — {2}{B}{B} Instant.
 * "This spell costs {X} less to cast, where X is your devotion to black. Destroy target creature."
 *
 * Proves the self-cast reduction reads the *caster's* devotion (an opponent's black permanents
 * don't count) and reduces only the generic part — the {B}{B} is always paid.
 */
class DragToTheUnderworldScenarioTest : ScenarioTestBase() {

    init {
        fun board(swamps: Int, withSpecter: Boolean) = scenario()
            .withPlayers()
            .withCardInHand(1, "Drag to the Underworld")
            .withLandsOnBattlefield(1, "Swamp", swamps)
            .apply { if (withSpecter) withCardOnBattlefield(1, "Hypnotic Specter") }
            .withCardOnBattlefield(2, "Sengir Vampire")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("devotion to black 2 — castable for {B}{B} and destroys the target") {
            val game = board(swamps = 2, withSpecter = true)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Drag to the Underworld", bears).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
        }

        test("an opponent's black permanents don't count — two Swamps can't pay {2}{B}{B}") {
            val game = board(swamps = 2, withSpecter = false)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Drag to the Underworld", bears).error shouldNotBe null
        }

        test("the reduction never touches the {B}{B} — one Swamp is not enough") {
            val game = board(swamps = 1, withSpecter = true)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Drag to the Underworld", bears).error shouldNotBe null
        }
    }
}
