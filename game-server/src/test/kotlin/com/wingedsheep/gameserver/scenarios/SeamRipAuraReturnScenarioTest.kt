package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Rules 303.4f and 303.4g when an Aura returns from exile via
 * Seam Rip's LeavesBattlefield trigger (ReturnLinkedExileUnderOwnersControl).
 *
 * 303.4f — If an Aura enters the battlefield by a means other than resolving as an
 * Aura spell and the effect does not specify what it enchants, the player putting it
 * onto the battlefield chooses a legal object or player for it to enchant.
 *
 * 303.4g — If there is no legal object or player for the Aura to enchant, it remains
 * in its current zone — here, that means it stays in exile.
 */
class SeamRipAuraReturnScenarioTest : ScenarioTestBase() {

    init {
        context("Seam Rip — Aura return from exile") {

            test("303.4f — Rescue returns under owner's control, owner chooses a creature to enchant") {
                // Setup:
                //  P1: Seam Rip on battlefield (with P2's Rescue pre-linked in exile), 1 Plains
                //  P2: Seam Rip in hand, 1 Plains, Grizzly Bears on battlefield (Rescue will attach here)
                //  P2's turn — P2 casts their Seam Rip, ETB targets P1's Seam Rip, exiles it.
                //  P1's Seam Rip LTB fires → returns Rescue → P2 (owner) must choose a target (303.4f).

                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Seam Rip")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInHand(2, "Seam Rip")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInExile(2, "Shardmage's Rescue")  // P2 owns the exiled Rescue
                    .withCardInLibrary(2, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withActivePlayer(2)
                    .build()

                // Link Shardmage's Rescue to P1's Seam Rip (simulates P1's Seam Rip having exiled it)
                val p1SeamRipId = game.findPermanent("Seam Rip")!!
                val rescueId = game.state.getExile(game.player2Id).first()
                val p2BearsId = game.findPermanent("Grizzly Bears")!!

                game.state = game.state.updateEntity(p1SeamRipId) { c ->
                    c.with(LinkedExileComponent(listOf(rescueId)))
                }

                withClue("pre-condition: P1's Seam Rip has 1 linked exile") {
                    game.state.getEntity(p1SeamRipId)!!
                        .get<LinkedExileComponent>()!!.exiledIds.size shouldBe 1
                }

                // P2 casts Seam Rip (an enchantment — no spell targets; ETB triggers later)
                game.castSpell(2, "Seam Rip")
                // P1 has no responses — pass
                game.passPriority()
                // Both players passed with Seam Rip on stack → it resolves and enters battlefield
                // After entry, ETB trigger fires: P2 must choose a target to exile
                game.resolveStack()

                // P2's Seam Rip ETB: choose P1's Seam Rip as the exile target
                withClue("P2's Seam Rip ETB should ask for an exile target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(p1SeamRipId))
                // Resolve: P1's Seam Rip is exiled. Its LTB fires, returning Rescue.
                game.resolveStack()

                withClue("P1's Seam Rip should have left the battlefield") {
                    game.state.getBattlefield().any { id ->
                        id == p1SeamRipId
                    } shouldBe false
                }

                // 303.4f: Rescue is an Aura returning under owner's (P2's) control —
                // P2 must choose a creature to enchant.
                withClue("Should have a pending decision for P2 to choose Rescue's enchant target") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision()!!.playerId shouldBe game.player2Id
                }

                // P2 selects Grizzly Bears
                game.selectTargets(listOf(p2BearsId))
                game.resolveStack()

                // Rescue should now be on the battlefield attached to Grizzly Bears
                val rescueOnBattlefield = game.state.getBattlefield().find { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Shardmage's Rescue"
                }
                withClue("Shardmage's Rescue should be on the battlefield") {
                    rescueOnBattlefield shouldNotBe null
                }

                withClue("Rescue should be attached to Grizzly Bears (AttachedToComponent)") {
                    game.state.getEntity(rescueOnBattlefield!!)
                        ?.get<AttachedToComponent>()?.targetId shouldBe p2BearsId
                }

                withClue("Grizzly Bears should list Rescue in its AttachmentsComponent") {
                    game.state.getEntity(p2BearsId)
                        ?.get<AttachmentsComponent>()
                        ?.attachedIds
                        ?.contains(rescueOnBattlefield) shouldBe true
                }

                withClue("Rescue should no longer be in exile") {
                    game.state.getExile(game.player2Id).none { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Shardmage's Rescue"
                    } shouldBe true
                }
            }

            test("303.4g — Rescue stays in exile when owner controls no legal enchant targets") {
                // Same setup but P2 has NO creatures on the battlefield.
                // When P1's Seam Rip is exiled, Rescue has no legal target and must stay in exile.

                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Seam Rip")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInHand(2, "Seam Rip")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    // P2 has NO creatures — Rescue cannot attach anywhere
                    .withCardInExile(2, "Shardmage's Rescue")
                    .withCardInLibrary(2, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withActivePlayer(2)
                    .build()

                val p1SeamRipId = game.findPermanent("Seam Rip")!!
                val rescueId = game.state.getExile(game.player2Id).first()

                game.state = game.state.updateEntity(p1SeamRipId) { c ->
                    c.with(LinkedExileComponent(listOf(rescueId)))
                }

                // P2 casts Seam Rip
                game.castSpell(2, "Seam Rip")
                game.passPriority()
                game.resolveStack()

                // P2's Seam Rip ETB: exile P1's Seam Rip
                withClue("ETB decision to choose exile target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(p1SeamRipId))
                game.resolveStack()

                withClue("P1's Seam Rip should be gone from battlefield") {
                    game.state.getBattlefield().any { it == p1SeamRipId } shouldBe false
                }

                // 303.4g: No decision should be pending — Rescue automatically stays in exile
                // because P2 controls no creatures to enchant.
                withClue("No decision should be pending (Rescue stays in exile per 303.4g)") {
                    game.hasPendingDecision() shouldBe false
                }

                withClue("Shardmage's Rescue should still be in exile") {
                    game.state.getExile(game.player2Id).any { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Shardmage's Rescue"
                    } shouldBe true
                }

                withClue("Shardmage's Rescue should NOT be on the battlefield") {
                    game.state.getBattlefield().none { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Shardmage's Rescue"
                    } shouldBe true
                }
            }
        }
    }
}
