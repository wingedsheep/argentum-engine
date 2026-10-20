package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Enchantment Alteration (LEG #53) — {U} Instant.
 * "Attach target Aura attached to a creature or land to another permanent of that type."
 *
 * "That type" comes from the Aura's host at resolution. Faith's Fetters ("Enchant permanent") could
 * legally enchant anything, so the offered hosts show the type restriction itself doing the work.
 */
class EnchantmentAlterationScenarioTest : ScenarioTestBase() {

    private fun TestGame.hostOf(id: EntityId) = state.getEntity(id)!!.get<AttachedToComponent>()?.targetId

    private fun TestGame.castOn(aura: EntityId) {
        val result = castSpell(1, "Enchantment Alteration", aura)
        withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        test("an Aura on a land moves only to another land") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Enchantment Alteration")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(2, "Forest")
                .withCardAttachedTo(1, "Faith's Fetters", "Forest")
                .withCardOnBattlefield(2, "Mountain")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val fetters = game.findPermanent("Faith's Fetters")!!
            val island = game.findPermanent("Island")!!
            val mountain = game.findPermanent("Mountain")!!

            game.castOn(fetters)

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("other lands only — not the Forest it's on, not Grizzly Bears") {
                decision.legalTargets[0]!!.shouldContainExactlyInAnyOrder(island, mountain)
            }
            game.selectTargets(listOf(mountain)).error shouldBe null
            game.resolveStack()
            game.hostOf(fetters) shouldBe mountain
        }

        test("an Aura on a creature moves only to another creature") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Enchantment Alteration")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardAttachedTo(1, "Faith's Fetters", "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val fetters = game.findPermanent("Faith's Fetters")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castOn(fetters)

            // Hill Giant is the only other creature, so the choice is still offered (not auto-made)
            // or applied directly; either way the Fetters must end up on it, never on the Island.
            game.getPendingDecision()?.let { decision ->
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.legalTargets[0]!!.shouldContainExactlyInAnyOrder(giant)
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()
            }
            game.hostOf(fetters) shouldBe giant
        }

        test("an Aura on a land creature may move to a creature or a land") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Enchantment Alteration")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(2, "Dryad Arbor")
                .withCardAttachedTo(1, "Faith's Fetters", "Dryad Arbor")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val fetters = game.findPermanent("Faith's Fetters")!!
            val island = game.findPermanent("Island")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castOn(fetters)

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0]!!.shouldContainExactlyInAnyOrder(island, bears)
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.hostOf(fetters) shouldBe bears
        }
    }
}
