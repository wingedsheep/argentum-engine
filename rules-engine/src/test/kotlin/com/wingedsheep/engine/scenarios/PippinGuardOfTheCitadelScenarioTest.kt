package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Pippin, Guard of the Citadel (LTR #218) — {W}{U} Halfling Soldier, 2/2.
 *
 * "{T}: Another target creature you control gains protection from the card type of your choice
 *  until end of turn."
 *
 * Exercises the Gap-8 chosen-card-type protection flow: activating the {T} ability presents a
 * ChooseOptionDecision over the protectable card types; choosing "Creature" grants the target a
 * floating `PROTECTION_FROM_CARDTYPE_CREATURE` keyword that the targeting / combat-damage /
 * block-evasion enforcement sites all honor.
 */
class PippinGuardOfTheCitadelScenarioTest : ScenarioTestBase() {

    private val tapAbilityId =
        cardRegistry.getCard("Pippin, Guard of the Citadel")!!.activatedAbilities.first().id

    init {
        context("Pippin, Guard of the Citadel — protection from the chosen card type") {

            test("grants protection from creatures: target can't be dealt combat damage by a creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pippin, Guard of the Citadel")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 — the creature we protect
                    .withCardOnBattlefield(2, "Hill Giant")     // 3/3 attacker
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pippin = game.findPermanent("Pippin, Guard of the Citadel")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // Activate {T}: target our own Grizzly Bears.
                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = pippin,
                        abilityId = tapAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("Activating Pippin's ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                // Resolution pauses for the card-type choice.
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                val creatureIndex = decision.options.indexOf("Creature")
                withClue("Creature must be an offered card type") { (creatureIndex >= 0) shouldBe true }
                game.submitDecision(OptionChosenResponse(decision.id, creatureIndex))

                withClue("Grizzly Bears gains protection from creatures") {
                    game.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_CARDTYPE_CREATURE") shouldBe true
                }

                // Player 2's Hill Giant (3/3) attacks; Grizzly Bears (2/2) blocks. Protection from
                // creatures prevents the combat damage from the Giant, so the Bears survive.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Hill Giant" to 1))
                withClue("Hill Giant attacks Player1: ${attack.error}") { attack.error shouldBe null }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val block = game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant")))
                withClue("Grizzly Bears may block Hill Giant: ${block.error}") { block.error shouldBe null }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Grizzly Bears survives — combat damage from the creature is prevented") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("grants protection from creatures: target can't be blocked by a creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pippin, Guard of the Citadel")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 attacker we protect
                    .withCardOnBattlefield(2, "Hill Giant")     // 3/3 would-be blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pippin = game.findPermanent("Pippin, Guard of the Citadel")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = pippin,
                        abilityId = tapAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("Activating Pippin's ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                val decision = game.getPendingDecision() as ChooseOptionDecision
                val creatureIndex = decision.options.indexOf("Creature")
                game.submitDecision(OptionChosenResponse(decision.id, creatureIndex))

                withClue("Grizzly Bears gains protection from creatures") {
                    game.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_CARDTYPE_CREATURE") shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Grizzly Bears" to 2))
                withClue("Grizzly Bears attacks: ${attack.error}") { attack.error shouldBe null }

                // Hill Giant is a creature; the protected Grizzly Bears can't be blocked by it.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val block = game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears")))
                withClue("Blocking the creature-protected attacker with a creature is illegal: ${block.error}") {
                    (block.error != null) shouldBe true
                }
            }
        }
    }
}
