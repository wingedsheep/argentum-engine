package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Winds of Qal Sisma (FRF #147, {1}{G} Instant).
 *
 *   Prevent all combat damage that would be dealt this turn.
 *   Ferocious — If you control a creature with power 4 or greater, instead prevent all combat
 *   damage that would be dealt this turn by creatures your opponents control.
 */
class WindsOfQalSismaScenarioTest : ScenarioTestBase() {

    init {
        test("without ferocious, all combat damage is prevented, your creatures' included") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Winds of Qal Sisma")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                // An opponent's 4-power creature doesn't turn ferocious on: "you control".
                .withCardOnBattlefield(2, "Craw Wurm", tapped = true)
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Winds of Qal Sisma").error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the unblocked Hill Giant's damage to the opponent is prevented") {
                game.getLifeTotal(2) shouldBe 20
            }
            withClue("the blocking Centaur Courser's damage to the Grizzly Bears is prevented") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
            withClue("the Grizzly Bears' damage to the Centaur Courser is prevented too") {
                (game.state.getEntity(courser)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            }
        }

        test("with ferocious, only the opponents' creatures' combat damage is prevented") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Winds of Qal Sisma")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Winds of Qal Sisma").error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Craw Wurm" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the unblocked Craw Wurm's 6 combat damage is dealt") {
                game.getLifeTotal(2) shouldBe 14
            }
            withClue("the blocking Centaur Courser's damage to the Grizzly Bears is prevented") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
            withClue("the Grizzly Bears' 2 damage to the Centaur Courser is dealt") {
                (game.state.getEntity(courser)?.get<DamageComponent>()?.amount ?: 0) shouldBe 2
            }
        }
    }
}
