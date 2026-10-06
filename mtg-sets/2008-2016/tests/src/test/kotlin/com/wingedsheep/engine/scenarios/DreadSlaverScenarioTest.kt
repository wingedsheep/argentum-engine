package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dread Slaver (AVR #98).
 *
 * {3}{B}{B} Creature — Zombie Horror 3/5
 * "Whenever a creature dealt damage by this creature this turn dies, return it to the battlefield
 *  under your control. That creature is a black Zombie in addition to its other colors and types."
 *
 * Covers the payoff (control change + colour/type riders that *add* rather than replace) and the
 * ruling that the ability still triggers when Dread Slaver dies simultaneously with its victim.
 */
class DreadSlaverScenarioTest : ScenarioTestBase() {

    init {
        context("Dread Slaver") {

            test("a creature it killed returns under your control as a black Zombie in addition") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dread Slaver")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Dread Slaver" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Dread Slaver"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")
                withClue("the Bears died and came back onto the battlefield") {
                    (bears != null) shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
                withClue("under Dread Slaver's controller, not its owner") {
                    game.state.getEntity(bears!!)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }
                val projected = game.state.projectedState
                withClue("black in addition to green") {
                    projected.hasColor(bears!!, Color.BLACK) shouldBe true
                    projected.hasColor(bears, Color.GREEN) shouldBe true
                }
                withClue("Zombie in addition to Bear") {
                    projected.hasSubtype(bears!!, "Zombie") shouldBe true
                    projected.hasSubtype(bears, "Bear") shouldBe true
                }
            }

            test("still triggers when Dread Slaver dies alongside the creature it damaged") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dread Slaver")
                    .withCardOnBattlefield(2, "Typhoid Rats") // 1/1 deathtouch
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Dread Slaver" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Typhoid Rats" to listOf("Dread Slaver"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("Dread Slaver itself stays dead") {
                    game.isInGraveyard(1, "Dread Slaver") shouldBe true
                }
                val rats = game.findPermanent("Typhoid Rats")
                withClue("the Rats returned under Dread Slaver's controller") {
                    (rats != null) shouldBe true
                    game.state.getEntity(rats!!)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }
            }
        }
    }
}
