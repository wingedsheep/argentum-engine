package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.roe.cards.RaidBombardment
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Raid Bombardment (ROE #161) — "Whenever a creature you control with power 2 or less attacks,
 * this enchantment deals 1 damage to the player or planeswalker that creature is attacking."
 *
 * Three players: each small attacker pings the player it attacks; a 3-power attacker doesn't
 * trigger. An attacker aimed at a battle triggers but does nothing (the card's ruling).
 */
class RaidBombardmentScenarioTest : ScenarioTestBase() {

    init {
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

        test("an attacker aimed at a battle deals no damage to anyone") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Raid Bombardment")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Invasion of Innistrad") // protected by the opponent
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.checkStateBasedActions()
            val battle = game.findPermanent("Invasion of Innistrad")!!
            fun defense() = game.state.getEntity(battle)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE)
            val before = defense()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                playerAttackers = emptyMap(),
                permanentAttackers = mapOf("Grizzly Bears" to "Invasion of Innistrad"),
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
            withClue("\"player or planeswalker\" is literal: the battle isn't hit") { defense() shouldBe before }
        }
    }
}
