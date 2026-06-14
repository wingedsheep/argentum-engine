package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Display of Power — "This spell can't be copied. Copy any number of target instant and/or
 * sorcery spells. You may choose new targets for the copies." (CR 707.10).
 *
 * Exercises the CopyEachTargetSpellEffect + the cantBeCopied flag.
 */
class DisplayOfPowerScenarioTest : ScenarioTestBase() {

    init {
        test("copies a targeted instant on the stack and retargets the copy") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Display of Power")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Grizzly Bears") // bolt target A (2/2)
                .withCardOnBattlefield(2, "Hill Giant")    // copy target B (3/3)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearsA = game.findPermanent("Grizzly Bears")!!
            val giantB = game.findPermanent("Hill Giant")!!

            // Cast Lightning Bolt at the Grizzly Bears — it sits on the stack.
            game.castSpell(1, "Lightning Bolt", bearsA).error shouldBe null

            // Cast Display of Power targeting the Bolt spell on the stack.
            game.castSpellTargetingStackSpell(1, "Display of Power", "Lightning Bolt").error shouldBe null

            // Resolve Display of Power — it pauses to retarget the copy of the Bolt.
            game.resolveStack()
            game.hasPendingDecision().shouldBeTrue()

            // Retarget the copy at the Hill Giant; then resolve the copy and the original.
            game.selectTargets(listOf(giantB)).error shouldBe null
            game.resolveStack()

            // Original Bolt killed the Bears; the copy killed the Giant.
            game.findPermanent("Grizzly Bears") shouldBe null
            game.findPermanent("Hill Giant") shouldBe null
        }

        test("Display of Power itself can't be copied") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Display of Power")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearsA = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Lightning Bolt", bearsA).error shouldBe null
            game.castSpellTargetingStackSpell(1, "Display of Power", "Lightning Bolt").error shouldBe null

            // The Display of Power spell entity carries the uncopiable marker.
            val displayId = game.state.stack.first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Display of Power"
            }
            game.state.getEntity(displayId)?.has<CantBeCopiedComponent>().shouldNotBeNull()
            game.state.getEntity(displayId)!!.has<CantBeCopiedComponent>().shouldBeTrue()
        }

        test("copies two targeted spells, each independently") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Display of Power")
                .withCardsInHand(1, "Lightning Bolt", 2)
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLifeTotal(2, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val player1Id = game.player1Id
            val player2Id = game.player2Id

            // Two Bolts on the stack, both aimed at the opponent's face.
            val bolts = game.findCardsInHand(1, "Lightning Bolt")
            game.execute(CastSpell(player1Id, bolts[0], listOf(ChosenTarget.Player(player2Id)))).error shouldBe null
            game.execute(CastSpell(player1Id, bolts[1], listOf(ChosenTarget.Player(player2Id)))).error shouldBe null

            // Display of Power targeting BOTH Bolt spells on the stack.
            val display = game.findCardsInHand(1, "Display of Power").first()
            val boltSpellIds = game.state.stack.filter {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
            }.map { ChosenTarget.Spell(it) }
            game.execute(CastSpell(player1Id, display, boltSpellIds)).error shouldBe null

            // Resolve: each copy keeps the opponent as target (decline retargeting by
            // re-selecting the same legal target) — the copies have a player target so the
            // engine inherits verbatim only if no retarget is offered; player targets are
            // offered, so answer each.
            var guard = 0
            while (game.state.stack.isNotEmpty() && guard++ < 20) {
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(player2Id))
                }
            }

            // 4 Bolts total resolved (2 originals + 2 copies) = 12 damage.
            game.getLifeTotal(2) shouldBe 8
        }
    }
}
