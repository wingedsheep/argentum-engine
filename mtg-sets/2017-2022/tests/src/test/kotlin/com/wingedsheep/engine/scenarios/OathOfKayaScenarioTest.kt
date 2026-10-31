package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Oath of Kaya (WAR #209) — {1}{W}{B} Legendary Enchantment.
 *
 *   When Oath of Kaya enters, it deals 3 damage to any target and you gain 3 life.
 *   Whenever an opponent attacks a planeswalker you control with one or more creatures, Oath of
 *   Kaya deals 2 damage to that player and you gain 2 life.
 */
class OathOfKayaScenarioTest : ScenarioTestBase() {

    init {
        test("enters: 3 damage to any target and you gain 3 life") {
            val game = scenario()
                .withPlayers("Kaya", "Opponent")
                .withCardInHand(1, "Oath of Kaya")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Oath of Kaya").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.findPermanent("Centaur Courser")!!)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Centaur Courser") shouldBe true
            game.getLifeTotal(1) shouldBe 23
        }

        test("an opponent attacking your planeswalker takes 2 and you gain 2") {
            val game = board()

            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Centaur Courser" to "Chandra, Flameshaper"),
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
        }

        test("attacking you, not a planeswalker, does not trigger it") {
            val game = board()

            game.declareAttackers(mapOf("Centaur Courser" to 1)).error shouldBe null
            game.resolveStack()

            withClue("the attacker is aimed at Kaya's controller, so nothing triggers") {
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }

    private fun board() = scenario()
        .withPlayers("Kaya", "Attacker")
        .withCardOnBattlefield(1, "Oath of Kaya")
        .withCardOnBattlefield(1, "Chandra, Flameshaper")
        .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
        .withActivePlayer(2)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()
}
