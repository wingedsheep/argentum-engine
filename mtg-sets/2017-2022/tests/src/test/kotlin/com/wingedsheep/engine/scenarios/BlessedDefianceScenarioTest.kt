package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Blessed Defiance (MID) — "Target creature you control gets +2/+0 and gains lifelink until end of
 * turn. When that creature dies this turn, create a 1/1 white Spirit creature token with flying."
 *
 * The pump and lifelink last until end of turn; the death clause is an entity-scoped delayed
 * trigger that fires for any death that turn (combat or not) and produces nothing if the creature
 * survives. The target must be a creature you control.
 */
class BlessedDefianceScenarioTest : ScenarioTestBase() {

    init {
        context("Blessed Defiance — pump, lifelink, and a Spirit on death") {

            test("grants +2/+0 and lifelink; combat damage gains that much life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blessed Defiance")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Blessed Defiance", targetId = bears).error shouldBe null
                game.resolveStack()

                val projected = StateProjector().project(game.state)
                withClue("a 2/2 becomes a 4/2 with lifelink") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("4 lifelink damage: opponent to 16, controller to 24") {
                    game.getLifeTotal(2) shouldBe 16
                    game.getLifeTotal(1) shouldBe 24
                }
                withClue("no death, no Spirit") {
                    game.findPermanents("Spirit Token").size shouldBe 0
                }
            }

            test("creates a 1/1 white flying Spirit when the pumped creature dies in combat") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blessed Defiance")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 -> 4/2
                    .withCardOnBattlefield(2, "Craw Wurm") // 6/4 blocker kills it
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Blessed Defiance", targetId = bears).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Craw Wurm" to listOf("Grizzly Bears")))
                    .error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                }
                game.resolveStack()

                withClue("the Bears died, lifelinked 4 damage into the Wurm, and left a Spirit") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.getLifeTotal(1) shouldBe 24
                    val spirits = game.findPermanents("Spirit Token")
                    spirits.size shouldBe 1
                    val spirit = spirits.single()
                    val projected = StateProjector().project(game.state)
                    projected.getPower(spirit) shouldBe 1
                    projected.getToughness(spirit) shouldBe 1
                    projected.hasKeyword(spirit, Keyword.FLYING) shouldBe true
                    projected.getColors(spirit) shouldBe setOf(Color.WHITE.name)
                    projected.isCreature(spirit) shouldBe true
                }
            }

            test("the Spirit still appears when the creature dies outside combat") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blessed Defiance")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Blessed Defiance", targetId = bears).error shouldBe null
                game.resolveStack()

                game.passPriority() // hand priority to the opponent so they can respond
                game.castSpell(2, "Murder", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("the delayed trigger watches the creature, not the combat") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.findPermanents("Spirit Token").size shouldBe 1
                }
            }

            test("no Spirit when the creature survives the turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blessed Defiance")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Blessed Defiance", targetId = bears).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)

                withClue("the Bears is alive, so nothing was created") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.findPermanents("Spirit Token").size shouldBe 0
                }
            }

            test("cannot target a creature an opponent controls") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blessed Defiance")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirBears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Blessed Defiance", targetId = theirBears).error shouldNotBe null
            }
        }
    }
}
