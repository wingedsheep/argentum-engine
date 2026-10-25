package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.VoloGuideToMonsters
import com.wingedsheep.mtg.sets.definitions.lrw.cards.WoodlandChangeling
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VoloGuideToMonstersScenarioTest : FunSpec({
    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(VoloGuideToMonsters, WoodlandChangeling))
            it.initMirrorMatch(Deck.of("Forest" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        d.putPermanentOnBattlefield(player, "Volo, Guide to Monsters")
        return d to player
    }

    /** Casts [name] from hand, resolves everything, and returns how many copies of it are on the battlefield. */
    fun castAndCount(d: GameTestDriver, player: EntityId, name: String, triggers: Boolean): Int {
        val card = d.putCardInHand(player, name)
        d.giveMana(player, Color.GREEN, 3)
        d.castSpell(player, card)
        d.state.stack.size shouldBe (if (triggers) 2 else 1)
        while (d.state.stack.isNotEmpty()) d.bothPass()
        return d.getPermanents(player).count { d.getCardName(it) == name }
    }

    test("a creature spell sharing no type with your creatures or graveyard is copied") {
        val (d, player) = setup()
        castAndCount(d, player, "Centaur Courser", triggers = true) shouldBe 2
    }

    test("a creature card in your graveyard sharing a type stops the copy") {
        val (d, player) = setup()
        d.putCardInGraveyard(player, "Centaur Courser")
        castAndCount(d, player, "Centaur Courser", triggers = false) shouldBe 1
    }

    test("a Changeling spell shares a type with Volo on the stack and is not copied") {
        val (d, player) = setup()
        castAndCount(d, player, "Woodland Changeling", triggers = false) shouldBe 1
    }

    test("a Changeling card in your graveyard shares a type with any creature spell") {
        val (d, player) = setup()
        d.putCardInGraveyard(player, "Woodland Changeling")
        castAndCount(d, player, "Centaur Courser", triggers = false) shouldBe 1
    }
})
