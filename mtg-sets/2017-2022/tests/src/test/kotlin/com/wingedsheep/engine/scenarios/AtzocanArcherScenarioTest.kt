package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.AtzocanArcher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AtzocanArcherScenarioTest : FunSpec({
    data class Setup(val driver: GameTestDriver, val player: EntityId, val archer: EntityId, val other: EntityId)

    fun setup(friendly: Boolean = false): Setup {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + AtzocanArcher)
            initMirrorMatch(Deck.of("Forest" to 30))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        val other = d.putCreatureOnBattlefield(if (friendly) player else d.getOpponent(player), "Grizzly Bears")
        val archer = d.putCardInHand(player, "Atzocan Archer")
        d.giveMana(player, Color.GREEN, 3)
        d.castSpell(player, archer).error shouldBe null
        d.bothPass()
        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        choice.targetRequirements.single().minTargets shouldBe 1
        (archer in choice.legalTargets.values.flatten()) shouldBe false
        (other in choice.legalTargets.values.flatten()) shouldBe true
        d.submitTargetSelection(player, listOf(other)).error shouldBe null
        return Setup(d, player, archer, other)
    }

    test("chooses another target before asking whether to fight on resolution") {
        val (d, player, archer, other) = setup()
        d.pendingDecision shouldBe null
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(player, true).error shouldBe null
        d.state.getEntity(archer)?.get<DamageComponent>()?.amount shouldBe 2
        d.state.getEntity(other)?.get<DamageComponent>()?.amount shouldBe 1
    }

    test("may fight another creature you control") {
        val (d, player, archer, other) = setup(friendly = true)
        d.bothPass()
        d.submitYesNo(player, true).error shouldBe null
        d.state.getEntity(archer)?.get<DamageComponent>()?.amount shouldBe 2
        d.state.getEntity(other)?.get<DamageComponent>()?.amount shouldBe 1
    }

    test("declining the fight deals no damage") {
        val (d, player, archer, other) = setup()
        d.bothPass()
        d.submitYesNo(player, false).error shouldBe null
        (d.state.getEntity(archer)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        (d.state.getEntity(other)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("an Archer that leaves before resolution deals no damage") {
        val (d, player, archer, other) = setup()
        d.moveToGraveyard(archer)
        d.bothPass()
        if (d.pendingDecision is YesNoDecision) d.submitYesNo(player, true).error shouldBe null
        (d.state.getEntity(other)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("an illegal target prevents the fight") {
        val (d, _, archer, other) = setup()
        d.moveToGraveyard(other)
        d.bothPass()
        d.pendingDecision shouldBe null
        (d.state.getEntity(archer)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("an opponent can respond before the controller decides whether to fight") {
        val (d, player, archer, other) = setup()
        val opponent = d.getOpponent(player)
        val growth = d.putCardInHand(opponent, "Giant Growth")
        d.giveMana(opponent, Color.GREEN, 1)
        d.passPriority(player).error shouldBe null
        d.castSpell(opponent, growth, listOf(other)).error shouldBe null
        d.bothPass()
        d.pendingDecision shouldBe null
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(player, false).error shouldBe null
        (d.state.getEntity(archer)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        (d.state.getEntity(other)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("with no other creature the trigger cannot be put on the stack") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + AtzocanArcher)
            initMirrorMatch(Deck.of("Forest" to 30))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        val archer = d.putCardInHand(player, "Atzocan Archer")
        d.giveMana(player, Color.GREEN, 3)
        d.castSpell(player, archer).error shouldBe null
        d.bothPass()
        d.pendingDecision shouldBe null
        d.state.stack.size shouldBe 0
        d.findPermanent(player, "Atzocan Archer") shouldBe archer
    }
})
