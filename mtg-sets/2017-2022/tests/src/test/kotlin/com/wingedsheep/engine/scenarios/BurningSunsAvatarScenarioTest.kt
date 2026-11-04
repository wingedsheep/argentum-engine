package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.ChandraNalaar
import com.wingedsheep.mtg.sets.definitions.xln.cards.BurningSunsAvatar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BurningSunsAvatarScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BurningSunsAvatar, ChandraNalaar))
        initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the optional creature can be the Avatar itself and both targets take damage") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val avatar = d.putCardInHand(player, "Burning Sun's Avatar")
        d.giveMana(player, Color.RED, 6)
        d.castSpell(player, avatar).error shouldBe null
        d.bothPass()

        val decision = d.pendingDecision as ChooseTargetsDecision
        decision.targetRequirements[0].minTargets shouldBe 1
        decision.targetRequirements[1].minTargets shouldBe 0
        d.submitMultiTargetSelection(player, mapOf(0 to listOf(opponent), 1 to listOf(avatar))).error shouldBe null
        d.bothPass()

        d.getLifeTotal(opponent) shouldBe 17
        d.state.getEntity(avatar)?.get<DamageComponent>()?.amount shouldBe 3
    }

    test("declining the creature target still damages the opponent") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val avatar = d.putCardInHand(player, "Burning Sun's Avatar")
        d.giveMana(player, Color.RED, 6)
        d.castSpell(player, avatar).error shouldBe null
        d.bothPass()
        d.submitMultiTargetSelection(player, mapOf(0 to listOf(opponent), 1 to emptyList())).error shouldBe null
        d.bothPass()

        d.getLifeTotal(opponent) shouldBe 17
        (d.state.getEntity(avatar)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("an illegal creature target does not stop the opponent damage") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val creature = d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val avatar = d.putCardInHand(player, "Burning Sun's Avatar")
        d.giveMana(player, Color.RED, 6)
        d.castSpell(player, avatar).error shouldBe null
        d.bothPass()
        d.submitMultiTargetSelection(player, mapOf(0 to listOf(opponent), 1 to listOf(creature))).error shouldBe null
        d.moveToGraveyard(creature)
        d.bothPass()

        d.getLifeTotal(opponent) shouldBe 17
    }

    test("an illegal planeswalker target preserves the creature target slot") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        // Any planeswalker is legal, including one the Avatar's controller owns.
        val planeswalker = d.putCardInHand(player, "Chandra Nalaar")
        d.giveMana(player, Color.RED, 5)
        d.castSpell(player, planeswalker).error shouldBe null
        d.bothPass()
        val avatar = d.putCardInHand(player, "Burning Sun's Avatar")
        d.giveMana(player, Color.RED, 6)
        d.castSpell(player, avatar).error shouldBe null
        d.bothPass()
        d.submitMultiTargetSelection(player, mapOf(0 to listOf(planeswalker), 1 to listOf(avatar))).error shouldBe null
        d.moveToGraveyard(planeswalker)
        d.bothPass()

        d.getLifeTotal(opponent) shouldBe 20
        d.state.getEntity(avatar)?.get<DamageComponent>()?.amount shouldBe 3
    }
})
