package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for the EOE bug:
 *     "Diplomatic Relations gives +1/+0 to TO creature if FROM creature is dead."
 *
 * Card reference — Diplomatic Relations ({2}{G}, Instant, EOE #177):
 *   "Target creature you control gets +1/+0 and gains vigilance until end of turn. It deals
 *    damage equal to its power to target creature an opponent controls."
 *
 * MTG rule 608.2b (partial illegal targets): on resolution, the spell re-checks each target;
 * any illegal target is removed. If at least one target is still legal, the spell still
 * resolves, but instructions that reference an illegal target are skipped. The +1/+0 + vigilance
 * instruction references the FROM target ("Target creature you control"). The damage
 * instruction's source ("It deals damage...") is the FROM target — and its amount is the FROM
 * target's power. Both instructions reference the FROM target, so if FROM is illegal on
 * resolution, neither instruction does anything to TO.
 *
 * Bug: when Alice's bears (FROM) die to Bolt before Diplomatic Relations resolves, the
 * ModifyStats sub-effect still applies its +1/+0 (and the deal-damage sub-effect still fires)
 * to TO (Bob's bears) — TO ends up either pumped, damaged, or both. Expected: nothing happens
 * to Bob's bears.
 */
class DiplomaticRelationsPartialIllegalTargetTest : ScenarioTestBase() {

    init {
        context("Diplomatic Relations — partial illegal target (FROM dies in response)") {

            test("Bob's bears must not be pumped or damaged when Alice's bears (FROM) is killed before resolution") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Diplomatic Relations")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Identify Alice's bears (FROM) and Bob's bears (TO) by controller.
                val aliceBears = game.state.getBattlefield().single { entityId ->
                    val container = game.state.getEntity(entityId) ?: return@single false
                    container.get<CardComponent>()?.name == "Grizzly Bears" &&
                        container.get<ControllerComponent>()?.playerId == game.player1Id
                }
                val bobBears = game.state.getBattlefield().single { entityId ->
                    val container = game.state.getEntity(entityId) ?: return@single false
                    container.get<CardComponent>()?.name == "Grizzly Bears" &&
                        container.get<ControllerComponent>()?.playerId == game.player2Id
                }

                // Alice casts Diplomatic Relations targeting her own bears (FROM = index 0)
                // and Bob's bears (TO = index 1).
                val diplomaticRelationsId = game.findCardsInHand(1, "Diplomatic Relations").single()
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        diplomaticRelationsId,
                        listOf(
                            ChosenTarget.Permanent(aliceBears),
                            ChosenTarget.Permanent(bobBears),
                        )
                    )
                )
                withClue("Diplomatic Relations should cast: ${cast.error}") {
                    cast.error shouldBe null
                }

                // Alice passes priority so Bob can respond.
                game.execute(PassPriority(game.player1Id))
                withClue("Bob should have priority to respond") {
                    game.state.priorityPlayerId shouldBe game.player2Id
                }

                // Bob casts Lightning Bolt targeting Alice's bears (the FROM target).
                val boltId = game.findCardsInHand(2, "Lightning Bolt").single()
                val bolt = game.execute(
                    CastSpell(
                        game.player2Id,
                        boltId,
                        listOf(ChosenTarget.Permanent(aliceBears))
                    )
                )
                withClue("Lightning Bolt should cast: ${bolt.error}") {
                    bolt.error shouldBe null
                }

                // Resolve the entire stack: Bolt resolves first (Alice's bears dies, FROM target
                // becomes illegal), then Diplomatic Relations re-validates and resolves with only
                // TO still legal.
                var iterations = 0
                while (game.state.stack.isNotEmpty() && iterations < 40) {
                    val priorityPlayer = game.state.priorityPlayerId ?: break
                    val r = game.execute(PassPriority(priorityPlayer))
                    if (r.error != null) break
                    iterations++
                }

                // Sanity: Alice's bears died to Bolt.
                withClue("Alice's bears should no longer be on the battlefield (killed by Bolt)") {
                    game.state.getBattlefield().contains(aliceBears) shouldBe false
                }
                withClue("Alice's Grizzly Bears should be in her graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                // Bob's bears MUST still be on the battlefield, with its printed 2/2 stats and
                // zero damage marked — Diplomatic Relations must do NOTHING to it once FROM died.
                withClue("Bob's Grizzly Bears should still be on the battlefield") {
                    game.state.getBattlefield().contains(bobBears) shouldBe true
                }

                val projected = StateProjector().project(game.state)
                withClue("Bob's bears power must be the printed 2 (no +1/+0 from Diplomatic Relations)") {
                    projected.getPower(bobBears) shouldBe 2
                }
                withClue("Bob's bears toughness must be the printed 2") {
                    projected.getToughness(bobBears) shouldBe 2
                }

                val damage = game.state.getEntity(bobBears)?.get<DamageComponent>()?.amount ?: 0
                withClue("Bob's bears must take NO damage from Diplomatic Relations (FROM is illegal)") {
                    damage shouldBe 0
                }
            }
        }
    }
}
