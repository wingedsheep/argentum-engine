package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.TreasureChest
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TreasureChestScenarioTest : FunSpec({
    val trinket = card("Test Chest Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    /** First seed whose d20 comes up [natural]. */
    fun seedFor(natural: Int): Long = (0L..10_000L).first { GameRng.seeded(it).nextInt(20).first + 1 == natural }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(TreasureChest, trinket))
            it.initMirrorMatch(Deck.of("Island" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        return d to d.activePlayer!!
    }

    /** Activate the Chest, force the d20's natural face, and resolve. */
    fun roll(d: GameTestDriver, player: EntityId, natural: Int): ExecutionResult {
        val chest = d.putPermanentOnBattlefield(player, "Treasure Chest")
        d.giveColorlessMana(player, 4)
        d.submit(ActivateAbility(player, chest, TreasureChest.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        (chest in d.getPermanents(player)) shouldBe false
        d.replaceState(d.state.copy(rng = GameRng.seeded(seedFor(natural))))
        return d.bothPass()
    }

    test("1 is a trap: you lose 3 life") {
        val (d, player) = setup()
        roll(d, player, natural = 1)
        d.getLifeTotal(player) shouldBe 17
    }

    test("20 finds an artifact card and may put it onto the battlefield") {
        val (d, player) = setup()
        val found = d.putCardOnTopOfLibrary(player, trinket.name)
        roll(d, player, natural = 20)
        d.submitCardSelection(player, listOf(found))
        d.submitYesNo(player, true)
        (found in d.getPermanents(player)) shouldBe true
    }

    test("20 with an artifact card declined puts it into your hand instead") {
        val (d, player) = setup()
        val found = d.putCardOnTopOfLibrary(player, trinket.name)
        roll(d, player, natural = 20)
        d.submitCardSelection(player, listOf(found))
        d.submitYesNo(player, false)
        (found in d.getHand(player)) shouldBe true
    }

    test("20 finding a nonartifact card puts it into your hand without asking") {
        val (d, player) = setup()
        val found = d.putCardOnTopOfLibrary(player, "Centaur Courser")
        roll(d, player, natural = 20)
        d.submitCardSelection(player, listOf(found))
        d.state.pendingDecision shouldBe null
        (found in d.getHand(player)) shouldBe true
    }
})
