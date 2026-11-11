package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Visions of Brutality (OGW #81) — {1}{B} Aura, Devoid.
 * "Enchant creature / Enchanted creature can't block. / Whenever enchanted creature deals damage,
 * its controller loses that much life."
 *
 * "Its controller" is the enchanted creature's controller, not the Aura's; and the damage that
 * kills the host still triggers the Aura that went to the graveyard with it.
 */
class VisionsOfBrutalityScenarioTest : ScenarioTestBase() {

    init {
        context("Visions of Brutality") {
            test("the enchanted creature can't block") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Visions of Brutality", "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
            }

            test("combat damage to a player makes the enchanted creature's controller lose that much") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Visions of Brutality", "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("Player takes 3 from the Giant") { game.getLifeTotal(1) shouldBe 17 }
                withClue("the Giant's controller loses 3, not the Aura's controller") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("a host that dies to the damage it deals still costs its controller that much life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Grizzly Bears")     // enchanted 2/2 attacker, dies
                    .withCardAttachedTo(1, "Visions of Brutality", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")        // 3/3 blocker
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Visions of Brutality") shouldBe true
                withClue("the dead Bears' controller loses the 2 it dealt") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
