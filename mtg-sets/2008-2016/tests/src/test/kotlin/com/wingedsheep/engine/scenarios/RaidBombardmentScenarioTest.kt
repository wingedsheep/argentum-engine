package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.roe.cards.RaidBombardment
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Raid Bombardment (ROE #161) — "Whenever a creature you control with power 2 or less attacks,
 * this enchantment deals 1 damage to the player or planeswalker that creature is attacking."
 *
 * Three players: each small attacker pings the player it attacks; a 3-power attacker doesn't
 * trigger.
 */
class RaidBombardmentScenarioTest : FunSpec({

    test("each power-2-or-less attacker pings its own defender; bigger attackers don't trigger") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + RaidBombardment)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1

        d.putPermanentOnBattlefield(p1, "Raid Bombardment")
        val bearsA = d.putCreatureOnBattlefield(p1, "Grizzly Bears").also { d.removeSummoningSickness(it) }
        val bearsB = d.putCreatureOnBattlefield(p1, "Grizzly Bears").also { d.removeSummoningSickness(it) }
        val big = d.putCreatureOnBattlefield(p1, "Centaur Courser").also { d.removeSummoningSickness(it) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bearsA to p2, bearsB to p3, big to p3)).error shouldBe null
        withClue("Centaur Courser has power 3") { d.stackSize shouldBe 2 }
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        d.getLifeTotal(p2) shouldBe 19
        d.getLifeTotal(p3) shouldBe 19
    }
})
