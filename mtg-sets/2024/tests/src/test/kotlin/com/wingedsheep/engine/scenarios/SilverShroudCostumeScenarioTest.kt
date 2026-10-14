package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Silver Shroud Costume (PIP #142) — {2} Artifact — Equipment.
 *
 *   Flash
 *   When this Equipment enters, attach it to target creature you control. That creature gains
 *   shroud until end of turn.
 *   Equipped creature can't be blocked.
 *   Equip {3}
 */
class SilverShroudCostumeScenarioTest : ScenarioTestBase() {

    init {
        test("entering attaches it to the target creature you control, which gains shroud") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Silver Shroud Costume")
                .withCardInHand(1, "Inspirit")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Silver Shroud Costume").error shouldBe null
            game.resolveStack()

            // Two creatures you control: the ETB trigger asks for its target.
            game.hasPendingDecision() shouldBe true
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            val costume = game.findPermanent("Silver Shroud Costume")!!
            game.state.getEntity(costume)?.get<AttachedToComponent>()?.targetId shouldBe bears
            game.state.projectedState.hasKeyword(bears, Keyword.SHROUD) shouldBe true
            game.state.projectedState.hasKeyword(giant, Keyword.SHROUD) shouldBe false

            // Shroud stops even its controller from targeting it.
            game.castSpell(1, "Inspirit", bears).error shouldNotBe null
        }

        test("the equipped creature can't be blocked") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Silver Shroud Costume", "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Savannah Lions" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
            // The unequipped attacker is still blockable.
            game.declareBlockers(mapOf("Hill Giant" to listOf("Savannah Lions"))).error shouldBe null
        }
    }
}
