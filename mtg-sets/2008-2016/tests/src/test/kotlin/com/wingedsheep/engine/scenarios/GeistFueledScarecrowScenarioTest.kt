package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Geist-Fueled Scarecrow (EMN #196) — {4} Artifact Creature — Scarecrow, 4/4.
 *
 *   Creature spells you cast cost {1} more to cast.
 *
 * Only its controller's creature spells are taxed; noncreature spells cost what they always did.
 */
class GeistFueledScarecrowScenarioTest : ScenarioTestBase() {

    private fun board(forests: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Geist-Fueled Scarecrow")
        .withCardInHand(1, "Grizzly Bears")
        .withCardInHand(1, "Giant Growth")
        .withLandsOnBattlefield(1, "Forest", forests)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a two-mana creature spell can't be cast with two lands") {
            val game = board(forests = 2)
            game.castSpell(1, "Grizzly Bears").error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("a two-mana creature spell costs three") {
            val game = board(forests = 3)
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("noncreature spells are not taxed") {
            val game = board(forests = 1)
            val scarecrow = game.findPermanent("Geist-Fueled Scarecrow")!!
            game.castSpell(1, "Giant Growth", scarecrow).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(scarecrow) shouldBe 7
        }
    }
}
