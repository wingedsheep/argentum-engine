package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Zagras, Thief of Heartbeats (ZNR #241) — {4}{B}{R} Legendary Creature — Vampire Rogue, 4/4.
 *
 *   Flying, deathtouch, haste. Other creatures you control have deathtouch.
 *   Whenever a creature you control deals combat damage to a planeswalker, destroy that planeswalker.
 *
 * The trigger's source filter makes the attacking creature the triggering entity, so "that
 * planeswalker" is read through `EffectTarget.DamageRecipient`.
 */
class ZagrasThiefOfHeartbeatsScenarioTest : ScenarioTestBase() {

    private fun board(activePlayer: Int = 1, builder: ScenarioBuilder.() -> Unit) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Zagras, Thief of Heartbeats")
        .apply(builder)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(activePlayer)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Zagras, Thief of Heartbeats") {

            test("another creature's combat damage to a planeswalker destroys that planeswalker") {
                val game = board {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    withCardOnBattlefield(2, "Jace, Mirror Mage")
                }
                val jace = game.findPermanent("Jace, Mirror Mage")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Grizzly Bears" to "Jace, Mirror Mage"),
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                withClue("2 damage leaves Jace on 2 loyalty — alive until the trigger resolves") {
                    game.state.getEntity(jace)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 2
                }
                game.resolveStack()

                game.isOnBattlefield("Jace, Mirror Mage") shouldBe false
                game.isInGraveyard(2, "Jace, Mirror Mage") shouldBe true
                withClue("the attacker is the triggering creature, not the destroyed permanent") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("combat damage to a player destroys nothing") {
                val game = board {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    withCardOnBattlefield(2, "Jace, Mirror Mage")
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.getLifeTotal(2) shouldBe 18
                game.isOnBattlefield("Jace, Mirror Mage") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("an opponent's creature damaging your planeswalker doesn't trigger") {
                val game = board(activePlayer = 2) {
                    withCardOnBattlefield(1, "Jace, Mirror Mage")
                    withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Grizzly Bears" to "Jace, Mirror Mage"),
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.isOnBattlefield("Jace, Mirror Mage") shouldBe true
            }

            test("other creatures you control have deathtouch") {
                val game = board {
                    withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    withCardOnBattlefield(2, "Hill Giant")
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("2 deathtouch damage kills the 3/3") { game.isOnBattlefield("Hill Giant") shouldBe false }
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
