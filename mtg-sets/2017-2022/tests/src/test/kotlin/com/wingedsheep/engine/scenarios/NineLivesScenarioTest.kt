package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nine Lives (M21 #28) — damage to you is prevented and puts an incarnation counter on it, one per
 * source; at nine counters it exiles itself, and leaving the battlefield loses you the game.
 */
class NineLivesScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Nine Lives")
            .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.incarnation(): Int =
            state.getEntity(findPermanent("Nine Lives")!!)?.get<CountersComponent>()
                ?.getCount(CounterType.INCARNATION) ?: 0

        test("a burn spell's damage is prevented and adds one counter") {
            val game = base().withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1).build()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
            game.incarnation() shouldBe 1
        }

        test("two attackers' combat damage adds one counter per source") {
            val game = base().withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant").build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(1) shouldBe 20
            game.incarnation() shouldBe 2
        }

        test("damage that can't be prevented is dealt and still adds a counter") {
            val game = base().withCardOnBattlefield(2, "Sunspine Lynx")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1).build()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
            game.incarnation() shouldBe 1
        }

        test("unpreventable combat damage is dealt and still adds a counter") {
            val game = base().withCardOnBattlefield(2, "Sunspine Lynx").build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Sunspine Lynx" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(1) shouldBe 15
            game.incarnation() shouldBe 1
        }

        test("the ninth counter exiles it and you lose the game") {
            val game = base().withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1).build()
            val nineLives = game.findPermanent("Nine Lives")!!
            game.state = game.state.updateEntity(nineLives) {
                it.with(CountersComponent().withAdded(CounterType.INCARNATION, 8))
            }
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.isInExile(1, "Nine Lives") shouldBe true
            game.state.winnerId shouldBe game.player2Id
        }
    }
}
