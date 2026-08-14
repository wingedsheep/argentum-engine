package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Royal Treatment — {G} Instant (WOE).
 *
 * Target creature you control gains hexproof until end of turn. Create a Royal Role token
 * attached to that creature. (Enchanted creature gets +1/+1 and has ward {1}.)
 *
 * Covers the hexproof grant, the Role token's +1/+1, the "only one of your Roles per creature"
 * replacement when a second Role lands on the same creature, and the you-control restriction.
 */
class RoyalTreatmentScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        test("grants hexproof and attaches a Royal Role that pumps the creature") {
            val g = scenario()
                .withPlayers()
                .withCardInHand(1, "Royal Treatment")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = g.findPermanent("Grizzly Bears")!!

            g.castSpell(1, "Royal Treatment", bears).error shouldBe null
            g.resolveStack()

            g.findPermanent("Royal Role") shouldNotBe null

            val projected = projector.project(g.state)
            // 2/2 base + the Role's +1/+1.
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
        }

        test("a second Role on the same creature replaces the first") {
            val g = scenario()
                .withPlayers()
                .withCardInHand(1, "Royal Treatment")
                .withCardInHand(1, "Monstrous Rage")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = g.findPermanent("Grizzly Bears")!!

            g.castSpell(1, "Royal Treatment", bears).error shouldBe null
            g.resolveStack()
            g.findPermanent("Royal Role") shouldNotBe null

            // Monstrous Rage's Monster Role lands on the same creature; every Role of mine on it
            // except the newest-timestamped one is put into the graveyard as a state-based action
            // (CR 303.7a / 704.5y), so the Royal Role falls off.
            g.castSpell(1, "Monstrous Rage", bears).error shouldBe null
            g.resolveStack()

            g.findPermanent("Monster Role") shouldNotBe null
            g.findPermanent("Royal Role") shouldBe null
        }

        test("cannot target a creature an opponent controls") {
            val g = scenario()
                .withPlayers()
                .withCardInHand(1, "Royal Treatment")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = g.findPermanent("Grizzly Bears")!!

            g.castSpell(1, "Royal Treatment", bears).error shouldNotBe null
            g.findPermanent("Royal Role") shouldBe null
        }
    }
}
