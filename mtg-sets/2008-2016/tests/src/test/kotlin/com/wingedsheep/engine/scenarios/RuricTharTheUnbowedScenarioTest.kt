package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ruric Thar, the Unbowed (DGM #99) — {4}{R}{G} 6/6 Legendary Creature — Ogre Warrior.
 *
 *   Reach, vigilance
 *   Ruric Thar attacks each combat if able.
 *   Whenever a player casts a noncreature spell, Ruric Thar deals 6 damage to that player.
 */
class RuricTharTheUnbowedScenarioTest : ScenarioTestBase() {
    init {
        test("a noncreature spell costs its caster 6 life, whoever they are") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Ruric Thar, the Unbowed")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.resolveStack()

            withClue("Bob, who cast Shock, takes 6 from Ruric Thar") { game.getLifeTotal(2) shouldBe 14 }
            withClue("Alice takes only Shock's 2") { game.getLifeTotal(1) shouldBe 18 }
        }

        test("its controller is punished too, but creature spells don't trigger it") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Ruric Thar, the Unbowed")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("a creature spell doesn't trigger Ruric Thar") { game.getLifeTotal(1) shouldBe 20 }

            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            withClue("Alice cast a noncreature spell and takes 6") { game.getLifeTotal(1) shouldBe 14 }
            game.getLifeTotal(2) shouldBe 18
        }

        test("has reach and vigilance and must attack each combat") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Ruric Thar, the Unbowed")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ruric = game.findPermanent("Ruric Thar, the Unbowed")!!
            game.state.projectedState.hasKeyword(ruric, Keyword.REACH) shouldBe true
            game.state.projectedState.hasKeyword(ruric, Keyword.VIGILANCE) shouldBe true

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val noAttack = game.execute(DeclareAttackers(playerId = game.player1Id, attackers = emptyMap()))
            withClue("declaring no attackers is illegal") { noAttack.error shouldNotBe null }
            game.declareAttackers(mapOf("Ruric Thar, the Unbowed" to 2)).error shouldBe null
        }
    }
}
