package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DieRolledEvent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.GoblinMorningstar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Goblin Morningstar: the ETB d20 always makes a 1/1 Goblin; only on 10—20 does the Equipment
 * attach itself to that token (the create-then-attach chain nested inside a results-table row).
 */
class GoblinMorningstarScenarioTest : FunSpec({

    /** First seed whose d20 comes up [natural]. */
    fun seedFor(natural: Int): Long = (0L..10_000L).first { GameRng.seeded(it).nextInt(20).first + 1 == natural }

    data class Setup(val d: GameTestDriver, val player: EntityId, val morningstar: EntityId, val goblin: EntityId)

    fun castAndRoll(natural: Int): Setup {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(GoblinMorningstar))
            it.initMirrorMatch(Deck.of("Mountain" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        val card = d.putCardInHand(player, "Goblin Morningstar")
        d.giveMana(player, Color.RED, 1)
        d.giveColorlessMana(player, 1)
        d.castSpell(player, card)
        d.bothPass()
        d.replaceState(d.state.copy(rng = GameRng.seeded(seedFor(natural))))
        d.bothPass().events.filterIsInstance<DieRolledEvent>().single().result shouldBe natural
        val creatures = d.getCreatures(player)
        creatures shouldHaveSize 1
        return Setup(d, player, d.findPermanent(player, "Goblin Morningstar")!!, creatures.single())
    }

    test("1-9 creates a Goblin token but leaves the Equipment unattached") {
        val (d, _, morningstar, goblin) = castAndRoll(natural = 4)
        d.state.getEntity(morningstar)?.get<AttachedToComponent>() shouldBe null
        val projected = d.state.projectedState
        projected.getPower(goblin) shouldBe 1
        projected.hasKeyword(goblin, Keyword.TRAMPLE) shouldBe false
    }

    test("10-20 creates a Goblin token and attaches the Equipment to it") {
        val (d, _, morningstar, goblin) = castAndRoll(natural = 15)
        d.state.getEntity(morningstar)?.get<AttachedToComponent>()?.targetId shouldBe goblin
        val projected = d.state.projectedState
        projected.getPower(goblin) shouldBe 2
        projected.getToughness(goblin) shouldBe 1
        projected.hasKeyword(goblin, Keyword.TRAMPLE) shouldBe true
    }
})
