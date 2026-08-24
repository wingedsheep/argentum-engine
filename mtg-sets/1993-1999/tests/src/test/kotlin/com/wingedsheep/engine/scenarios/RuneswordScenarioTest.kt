package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Runesword (DRK #107).
 *
 * {6} Artifact
 * "{3}, {T}: Target attacking creature gets +2/+0 until end of turn. When that creature leaves the
 *  battlefield this turn, sacrifice this artifact. If the creature deals damage to a creature this
 *  turn, the creature dealt damage can't be regenerated this turn. If a creature dealt damage by
 *  the targeted creature would die this turn, exile that creature instead."
 *
 * One activation, three riders that outlive it — so each test checks a different rider rather than
 * just the pump.
 */
class RuneswordScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        fun swordAbilityId() =
            cardRegistry.getCard("Runesword")!!.script.activatedAbilities[0].id

        /** Attack with [attacker], then point the Sword at it. */
        fun TestGame.attackAndArm(attacker: String) {
            declareAttackers(mapOf(attacker to 2)).error shouldBe null
            // The Sword's controller is the active player, so they already hold priority here —
            // passing would hand it to the defender and the activation would be rejected.
            val sword = findPermanent("Runesword")!!
            execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = sword,
                    abilityId = swordAbilityId(),
                    targets = listOf(entityIdToChosenTarget(state, findPermanent(attacker)!!))
                )
            ).error shouldBe null
            resolveStack()
        }

        context("Runesword") {

            test("pumps the targeted attacker by +2/+0") {
                val game = scenario()
                    .withPlayers("Swordbearer", "Defender")
                    .withCardOnBattlefield(1, "Runesword")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.attackAndArm("Grizzly Bears")

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = projector.project(game.state)
                withClue("2/2 plus +2/+0") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 2
                }
            }

            test("when the armed creature leaves the battlefield, the Sword is sacrificed") {
                val game = scenario()
                    .withPlayers("Swordbearer", "Defender")
                    .withCardOnBattlefield(1, "Runesword")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.attackAndArm("Grizzly Bears")
                game.findPermanent("Runesword").shouldNotBeNull()

                // 3 damage kills the now-4/2 Bears.
                game.castSpell(1, "Lightning Bolt", targetId = game.findPermanent("Grizzly Bears")!!)
                    .error shouldBe null
                game.resolveStack()

                withClue("the armed creature is gone") {
                    game.findPermanent("Grizzly Bears").shouldBeNull()
                }
                withClue("so the delayed trigger sacrificed the Sword") {
                    game.findPermanent("Runesword").shouldBeNull()
                }
            }

            test("a creature the armed creature kills is exiled instead of dying") {
                // Bears become 4/2 and are blocked by a 2/3 Hurloon Minotaur: 4 damage kills it,
                // and the granted rider replaces that death with exile.
                val game = scenario()
                    .withPlayers("Swordbearer", "Defender")
                    .withCardOnBattlefield(1, "Runesword")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Hurloon Minotaur")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.attackAndArm("Grizzly Bears")

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hurloon Minotaur" to listOf("Grizzly Bears")))
                    .error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the blocker took 4 and left the battlefield") {
                    game.findPermanent("Hurloon Minotaur").shouldBeNull()
                }
                withClue("but to exile, not the graveyard") {
                    game.isInExile(2, "Hurloon Minotaur") shouldBe true
                    game.isInGraveyard(2, "Hurloon Minotaur") shouldBe false
                }
            }
        }
    }
}
