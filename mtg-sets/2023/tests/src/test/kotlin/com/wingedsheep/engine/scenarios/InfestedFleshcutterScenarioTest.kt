package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Infested Fleshcutter (ONE #17) — {1}{W} Artifact — Equipment
 *   Equipped creature gets +2/+0.
 *   Whenever equipped creature attacks, create a 1/1 colorless Phyrexian Mite artifact creature
 *   token with toxic 1 and "This token can't block."
 *   Equip {2}{W}
 */
class InfestedFleshcutterScenarioTest : ScenarioTestBase() {

    init {
        test("equipped creature gets +2/+0 and attacking with it creates a Phyrexian Mite") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Infested Fleshcutter", "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 2
            game.findAllPermanents("Phyrexian Mite") shouldHaveSize 0

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            val mites = game.findAllPermanents("Phyrexian Mite")
            mites shouldHaveSize 1
            game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD)).contains(mites.single()) shouldBe true
            game.getLifeTotal(2) shouldBe 16
        }

        test("an unequipped attacker creates no Mite") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Infested Fleshcutter")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.findAllPermanents("Phyrexian Mite") shouldHaveSize 0
            game.getLifeTotal(2) shouldBe 18
        }
    }
}
