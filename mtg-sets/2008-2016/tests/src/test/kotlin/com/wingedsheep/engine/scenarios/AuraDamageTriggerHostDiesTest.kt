package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine test: an Aura's "whenever enchanted creature deals / is dealt damage" trigger fires for
 * the very damage that kills its host.
 *
 * The ability triggers as the damage is dealt (CR 603.2), while the Aura is still attached. But the
 * creature's lethal-damage death and the Aura's own trip to the graveyard (CR 704.5m) happen in one
 * state-based-action pass before triggers are detected, so the detector has to rebuild the departed
 * Aura from its exit snapshot — its abilities, and the controller it had on the battlefield
 * (CR 603.10, 603.3a) — rather than from the live attachment index, which no longer links it to anything.
 */
class AuraDamageTriggerHostDiesTest : ScenarioTestBase() {

    init {
        context("deals-damage Aura (Spirit Link)") {
            test("trading in combat still gains the Aura's controller that much life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")     // 2/2 attacker, dies
                    .withCardAttachedTo(1, "Spirit Link", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Wind Drake")        // 2/2 blocker, dies
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wind Drake" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("Both creatures and the Aura are in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Wind Drake") shouldBe true
                    game.isInGraveyard(1, "Spirit Link") shouldBe true
                }
                withClue("The 2 damage the dying host dealt still triggered Spirit Link — once") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("an Aura on an opponent's creature triggers for its own controller after the host dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Grizzly Bears")     // the opponent's attacker, dies
                    .withCardAttachedTo(1, "Spirit Link", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")        // 3/3 blocker, survives
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
                withClue("Spirit Link's controller — not the host's — gains the 2") {
                    game.getLifeTotal(1) shouldBe 22
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("a host that survives triggers exactly once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")        // 3/3 attacker, survives
                    .withCardAttachedTo(1, "Spirit Link", "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")     // 2/2 blocker, dies
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.isOnBattlefield("Spirit Link") shouldBe true
                game.getLifeTotal(1) shouldBe 23
            }
        }

        context("is-dealt-damage Aura (Ragged Veins)") {
            test("lethal combat damage to the host still makes its controller lose that much life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")        // 3/3 attacker
                    .withCardOnBattlefield(2, "Grizzly Bears")     // 2/2 blocker, dies
                    .withCardAttachedTo(1, "Ragged Veins", "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Ragged Veins") shouldBe true
                withClue("The dead Bears' controller loses the 3 it was dealt") {
                    game.getLifeTotal(2) shouldBe 17
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
