package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Metamorphic Alteration (M19 #60) — {1}{U} Enchantment — Aura.
 * "Enchant creature / As this Aura enters, choose a creature. / Enchanted creature is a copy of the
 * chosen creature."
 *
 * Rulings (2018-07-13): any creature on the battlefield may be chosen; the copiable values are locked
 * in as it is chosen, so the chosen creature leaving doesn't change the copy; the copy is the Aura's
 * own static, so it ends when the Aura leaves and moves with the Aura to a new host.
 */
class MetamorphicAlterationScenarioTest : ScenarioTestBase() {

    private fun TestGame.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    private fun TestGame.cast(spell: String, target: EntityId) {
        val result = castSpell(1, spell, target)
        withClue("casting $spell should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    /** Alice's Grizzly Bears, Bob's Serra Angel and Hill Giant; Alice holds the Alteration. */
    private fun board(vararg extraInHand: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Metamorphic Alteration")
        .apply { extraInHand.forEach { withCardInHand(1, it) } }
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Serra Angel")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast the Alteration on [host], choosing [chosen] as it enters. */
    private fun TestGame.alter(host: EntityId, chosen: EntityId, offered: List<EntityId>): EntityId {
        cast("Metamorphic Alteration", host)
        val choice = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        withClue("any creature on the battlefield may be chosen") {
            choice.options.shouldContainExactlyInAnyOrder(offered)
        }
        selectCards(listOf(chosen)).error shouldBe null
        return findPermanent("Metamorphic Alteration")!!
    }

    init {
        test("the enchanted creature becomes a copy of the chosen creature, locked in when chosen") {
            val game = board("Doom Blade")
            val bears = game.findPermanent("Grizzly Bears")!!
            val angel = game.findPermanent("Serra Angel")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.alter(host = bears, chosen = angel, offered = listOf(bears, angel, giant))

            withClue("Grizzly Bears is now a Serra Angel: name, 4/4, flying, vigilance") {
                game.name(bears) shouldBe "Serra Angel"
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            }

            game.cast("Doom Blade", angel)
            game.isInGraveyard(2, "Serra Angel") shouldBe true
            withClue("the chosen creature leaving doesn't change the copy") {
                game.name(bears) shouldBe "Serra Angel"
                game.state.projectedState.getPower(bears) shouldBe 4
            }
        }

        test("the copy ends when the Aura leaves the battlefield") {
            val game = board("Disenchant")
            val bears = game.findPermanent("Grizzly Bears")!!
            val angel = game.findPermanent("Serra Angel")!!
            val giant = game.findPermanent("Hill Giant")!!
            val alteration = game.alter(host = bears, chosen = giant, offered = listOf(bears, angel, giant))
            game.name(bears) shouldBe "Hill Giant"

            game.cast("Disenchant", alteration)

            game.isInGraveyard(1, "Metamorphic Alteration") shouldBe true
            withClue("Grizzly Bears is itself again") {
                game.name(bears) shouldBe "Grizzly Bears"
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
        }

        test("moving the Aura moves the locked-in copy to the new host") {
            val game = board("Doom Blade", "Enchantment Alteration")
            val bears = game.findPermanent("Grizzly Bears")!!
            val angel = game.findPermanent("Serra Angel")!!
            val giant = game.findPermanent("Hill Giant")!!
            val alteration = game.alter(host = bears, chosen = angel, offered = listOf(bears, angel, giant))
            game.cast("Doom Blade", angel)

            game.cast("Enchantment Alteration", alteration)
            val move = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("the Aura can move to another creature, not the one it's on") {
                move.legalTargets[0]!!.shouldContainExactlyInAnyOrder(giant)
            }
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(alteration)!!.get<AttachedToComponent>()!!.targetId shouldBe giant
            withClue("the old host reverts") {
                game.name(bears) shouldBe "Grizzly Bears"
                game.state.projectedState.getPower(bears) shouldBe 2
            }
            withClue("the new host is a copy of the Serra Angel chosen earlier, though it's gone") {
                game.name(giant) shouldBe "Serra Angel"
                game.state.projectedState.getPower(giant) shouldBe 4
                game.state.projectedState.hasKeyword(giant, Keyword.FLYING) shouldBe true
            }
        }
    }
}
