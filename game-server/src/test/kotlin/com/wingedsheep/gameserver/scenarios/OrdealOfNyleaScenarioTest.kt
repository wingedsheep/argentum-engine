package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Ordeal of Nylea.
 *
 * Card reference:
 * - Ordeal of Nylea ({1}{G}): Enchantment — Aura
 *   "Enchant creature"
 *   "Whenever enchanted creature attacks, put a +1/+1 counter on it. Then if it has three or more +1/+1 counters on it, sacrifice this Aura."
 *   "When you sacrifice this Aura, search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."
 */
class OrdealOfNyleaScenarioTest : ScenarioTestBase() {

    init {
        context("Ordeal of Nylea basic functionality") {
            test("enchanted creature gets +1/+1 counter when attacking") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Ordeal of Nylea")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 creature
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Ordeal of Nylea targeting Grizzly Bears
                val grizzlyBearsID = game.findPermanent("Grizzly Bears")!!
                val castResult = game.castSpell(1, "Ordeal of Nylea", grizzlyBearsID)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Grizzly Bears
                val attackResult = game.declareAttackers(mapOf("Grizzly Bears" to 2))
                withClue("Declaring Grizzly Bears as attacker should succeed") {
                    attackResult.error shouldBe null
                }

                // Trigger should fire and resolve
                game.resolveStack()

                // Advance through combat
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Check if Grizzly Bears has a +1/+1 counter
                val grizzlyBearsAfter = game.findPermanent("Grizzly Bears")!!
                val counters = game.state.getEntity(grizzlyBearsAfter)?.get<CountersComponent>()
                counters shouldNotBe null
                counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }

        }
    }
}
