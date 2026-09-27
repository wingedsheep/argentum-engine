package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kusari-Gama (CHK #260).
 *
 * {3} Artifact — Equipment
 * "Equipped creature has "{2}: This creature gets +1/+0 until end of turn."
 *  Whenever equipped creature deals damage to a blocking creature, this Equipment deals that much
 *  damage to each other creature defending player controls.
 *  Equip {3}"
 *
 * The load-bearing case is the blocker the damage *kills*: combat-damage state-based actions move
 * it — and tear down its blocking status — before the trigger is detected, so the trigger only
 * fires if "blocking creature" is answered from the damage event's last-known information
 * (CR 603.10).
 */
class KusariGamaScenarioTest : ScenarioTestBase() {

    init {
        context("Kusari-Gama") {

            test("killing the blocker still triggers, damaging each other creature defending player controls") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")        // 3/3 attacker
                    .withCardAttachedTo(1, "Kusari-Gama", "Hill Giant")
                    .withCardOnBattlefield(1, "Llanowar Elves")    // our own creature — untouched
                    .withCardOnBattlefield(2, "Grizzly Bears")     // 2/2 blocker, dies to the 3
                    .withCardOnBattlefield(2, "Centaur Courser")   // 3/3 — dies to the trigger's 3
                    .withCardOnBattlefield(2, "Craw Wurm")         // 6/4 — survives the trigger's 3
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

                withClue("The blocker died to the equipped creature's combat damage") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("The trigger dealt 3 to each other creature the defending player controls") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                }
                withClue("Creatures the attacking player controls are not dealt damage") {
                    game.isOnBattlefield("Llanowar Elves") shouldBe true
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            // The Wurm's 6 also kills the equipped Giant, and the same state-based-action pass
            // unattaches Kusari-Gama before triggers are detected — the trigger still fires because
            // it triggered when the damage was dealt (CR 603.2), while the Giant was equipped.
            test("a surviving blocker is not dealt the trigger's damage again, even when the equipped creature dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")        // 3/3 attacker
                    .withCardAttachedTo(1, "Kusari-Gama", "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")         // 6/4 blocker, survives the 3
                    .withCardOnBattlefield(2, "Wind Drake")        // 2/2 — dies to the trigger's 3
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Craw Wurm" to listOf("Hill Giant"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("Each *other* creature is dealt 3") {
                    game.isInGraveyard(2, "Wind Drake") shouldBe true
                }
                withClue("The blocker took 3 from the Giant only — a second 3 would have been lethal") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                }
                withClue("The equipped Giant traded with the Wurm") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
            }

            test("damage to a player does not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Kusari-Gama", "Hill Giant")
                    .withCardOnBattlefield(2, "Wind Drake")        // doesn't block
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("Unblocked damage to the player is not damage to a blocking creature") {
                    game.getLifeTotal(2) shouldBe 17
                    game.isOnBattlefield("Wind Drake") shouldBe true
                }
            }
        }
    }
}
