package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dead-Iron Sledge (MRD #162).
 *
 * {1} Artifact — Equipment
 * "Whenever equipped creature blocks or becomes blocked by a creature, destroy both creatures.
 *  Equip {2}"
 *
 * The trigger is granted to the equipped creature, so it fires off *that* creature's combat and
 * "destroy both creatures" resolves from the creature's own perspective — the equipped creature
 * (Self) and the combat partner (TriggeringEntity). Both directions of the pairing are covered:
 * the equipped creature attacking and becoming blocked, and the equipped creature blocking.
 */
class DeadIronSledgeScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Dead-Iron Sledge").activatedAbilities[0].id
    }

    init {
        context("Dead-Iron Sledge") {

            test("equipped attacker that becomes blocked destroys both creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")      // 3/3 attacker
                    .withCardOnBattlefield(1, "Dead-Iron Sledge")
                    .withLandsOnBattlefield(1, "Mountain", 2)    // Equip {2}
                    .withCardOnBattlefield(2, "Wind Drake")      // 2/2 blocker
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val sledge = game.findPermanent("Dead-Iron Sledge")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sledge,
                        abilityId = equipAbilityId,
                        targets = listOf(ChosenTarget.Permanent(giant))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wind Drake" to listOf("Hill Giant"))).error shouldBe null

                // Put the granted trigger on the stack and resolve it, still in declare-blockers —
                // before combat damage could account for either death.
                game.passPriority()
                game.resolveStack()

                withClue("Both the equipped creature and the blocker are destroyed") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Wind Drake") shouldBe false
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInGraveyard(2, "Wind Drake") shouldBe true
                }
                withClue("The Equipment itself survives, merely unattached") {
                    game.isOnBattlefield("Dead-Iron Sledge") shouldBe true
                }
            }

            test("equipped blocker destroys both creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")      // 3/3 blocker, ours
                    .withCardAttachedTo(1, "Dead-Iron Sledge", "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2 ground attacker, theirs
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null

                game.passPriority()
                game.resolveStack()

                withClue("Blocking triggers the Sledge just as becoming blocked does") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("an unequipped Sledge destroys nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Dead-Iron Sledge")   // never attached
                    .withCardOnBattlefield(2, "Wind Drake")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wind Drake" to listOf("Hill Giant"))).error shouldBe null

                game.passPriority()
                game.resolveStack()

                withClue("No creature carries the granted trigger, so both survive the block") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isOnBattlefield("Wind Drake") shouldBe true
                }
            }
        }
    }
}
