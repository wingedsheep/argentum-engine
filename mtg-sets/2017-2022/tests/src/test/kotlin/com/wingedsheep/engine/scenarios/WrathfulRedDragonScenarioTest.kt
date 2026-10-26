package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wrathful Red Dragon (CLB #207) — {3}{R}{R} Creature — Dragon, 5/5.
 *
 *   Flying
 *   Whenever a Dragon you control is dealt damage, it deals that much damage to any target that
 *   isn't a Dragon.
 *
 * The target restriction is `Not(HasSubtype(Dragon))` over an any-target requirement: players
 * (which have no subtypes) and non-Dragon creatures stay legal, Dragons don't. The reflected
 * amount is the damage the Dragon received.
 */
class WrathfulRedDragonScenarioTest : ScenarioTestBase() {

    init {
        test("a damaged Dragon reflects that much damage at a non-Dragon target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Wrathful Red Dragon")
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(2, "Shivan Dragon")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wrathful = game.findPermanent("Wrathful Red Dragon")!!
            val shivan = game.findPermanent("Shivan Dragon")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Shock", wrathful).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets.getValue(0)
            withClue("players and non-Dragon creatures are legal targets") {
                legal shouldContain game.player2Id
                legal shouldContain bears
            }
            withClue("Dragons — including the damaged one — are not") {
                legal shouldNotContain shivan
                legal shouldNotContain wrathful
            }

            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("Shock's 2 damage is reflected at Player2") {
                game.getLifeTotal(2) shouldBe 18
            }
        }
    }
}
