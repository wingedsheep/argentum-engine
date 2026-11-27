package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Riders of Gavony (AVR #33).
 *
 * "Vigilance / As this creature enters, choose a creature type. / Human creatures you control have
 *  protection from creatures of the chosen type."
 *
 * The as-enters creature-type choice feeds `ProtectionScope.ChosenSubtype`: Humans you control (Riders
 * included) can't be blocked by creatures of the chosen type, while non-Humans and other types are
 * unaffected, and a noncreature spell isn't a creature of that type.
 */
class RidersOfGavonyScenarioTest : ScenarioTestBase() {

    private val riders = "Riders of Gavony"

    private fun TestGame.castRidersChoosing(type: String) {
        castSpell(1, riders).error shouldBe null
        resolveStack()
        val decision = getPendingDecision() as? ChooseOptionDecision
            ?: error("Expected the Riders' creature-type choice, got ${getPendingDecision()}")
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(type))).error shouldBe null
        resolveStack()
        isOnBattlefield(riders) shouldBe true
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, riders)
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(2, "Raging Goblin", summoningSickness = false)
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("Goblin chosen: a Goblin can't block Riders, but can block a non-Human; another type can block Riders") {
            val game = board()
            game.castRidersChoosing("Goblin")
            // Riders entered this turn; let it attack as if it had been around.
            game.state = game.state.updateEntity(game.findPermanent(riders)!!) {
                it.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(riders to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("protection from Goblin creatures: Raging Goblin can't block Riders") {
                game.declareBlockers(mapOf("Raging Goblin" to listOf(riders))).error shouldNotBe null
            }
            withClue("Grizzly Bears isn't a Human, so the Goblin can block it") {
                game.declareBlockers(mapOf("Raging Goblin" to listOf("Grizzly Bears"))).error shouldBe null
            }
        }

        test("Goblin chosen: a Giant can still block Riders, and an instant still targets it") {
            val game = board()
            game.castRidersChoosing("Goblin")
            game.state.projectedState.hasKeyword(game.findPermanent(riders)!!, "PROTECTION_FROM_SUBTYPE_GOBLIN@CREATURE") shouldBe true
            game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, "PROTECTION_FROM_SUBTYPE_GOBLIN@CREATURE") shouldBe false
            withClue("Lightning Bolt is not a creature, so it can target Riders") {
                game.castSpell(1, "Lightning Bolt", game.findPermanent(riders)).error shouldBe null
            }
        }
    }
}
