package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Oketra the True (AKH #21) — {3}{W} 3/6 Legendary Creature — God.
 *
 *   Double strike, indestructible
 *   Oketra can't attack or block unless you control at least three other creatures.
 *   {3}{W}: Create a 1/1 white Warrior creature token with vigilance.
 *
 * Proves the combat restriction's count leaves Oketra herself out ("other"), and that her own
 * Warrior tokens count toward enabling her.
 */
class OketraTheTrueScenarioTest : ScenarioTestBase() {

    init {
        context("Oketra the True") {

            test("cannot attack with only two other creatures; Oketra herself doesn't count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Oketra the True", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                withClue("three creatures total, but only two are 'other'") {
                    game.declareAttackers(mapOf("Oketra the True" to 2)).error shouldNotBe null
                }
            }

            test("a Warrior token from her ability makes the third other creature, so she can attack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Oketra the True", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oketra = game.findPermanent("Oketra the True")!!
                val abilityId = cardRegistry.getCard("Oketra the True")!!
                    .script.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = oketra,
                        abilityId = abilityId,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.findPermanent("Warrior Token") shouldNotBe null

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                game.declareAttackers(mapOf("Oketra the True" to 2)).error shouldBe null
            }

            test("cannot block with two other creatures, can block with three") {
                val blocked = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Oketra the True", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(1, "Craw Wurm", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()
                blocked.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                blocked.declareAttackers(mapOf("Craw Wurm" to 2)).error shouldBe null
                blocked.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                blocked.declareBlockers(mapOf("Oketra the True" to listOf("Craw Wurm"))).error shouldNotBe null

                val allowed = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Oketra the True", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(1, "Craw Wurm", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()
                allowed.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                allowed.declareAttackers(mapOf("Craw Wurm" to 2)).error shouldBe null
                allowed.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                allowed.declareBlockers(mapOf("Oketra the True" to listOf("Craw Wurm"))).error shouldBe null
            }
        }
    }
}
