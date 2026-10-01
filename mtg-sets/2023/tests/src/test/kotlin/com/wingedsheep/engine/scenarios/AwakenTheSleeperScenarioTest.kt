package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.AwakenTheSleeper
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Awaken the Sleeper — {3}{R} Sorcery (ONE #119)
 *
 * "Gain control of target creature until end of turn. Untap that creature. It gains haste until
 * end of turn. If it's equipped, you may destroy all Equipment attached to that creature."
 */
class AwakenTheSleeperScenarioTest : FunSpec({

    val testEquipment = CardDefinition(
        name = "Test Sword",
        manaCost = ManaCost.parse("{1}"),
        typeLine = TypeLine.parse("Artifact — Equipment"),
        oracleText = "Equip {1}",
        script = CardScript()
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AwakenTheSleeper + testEquipment)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.attachEquipment(equipmentId: EntityId, creatureId: EntityId) {
        var newState = state.updateEntity(equipmentId) { it.with(AttachedToComponent(creatureId)) }
        newState = newState.updateEntity(creatureId) { container ->
            val existing = container.get<AttachmentsComponent>()
            container.with(AttachmentsComponent((existing?.attachedIds ?: emptyList()) + equipmentId))
        }
        replaceState(newState)
    }

    fun GameTestDriver.cast(you: EntityId, target: EntityId) {
        val spell = putCardInHand(you, "Awaken the Sleeper")
        giveMana(you, Color.RED, 4)
        castSpellWithTargets(you, spell, listOf(ChosenTarget.Permanent(target))).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("steals, untaps and hastes an unequipped creature without asking about Equipment") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)

        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        driver.tapPermanent(courser)

        driver.cast(you, courser)

        driver.pendingDecision shouldBe null
        driver.state.projectedState.getController(courser) shouldBe you
        driver.isTapped(courser) shouldBe false
        driver.state.projectedState.hasKeyword(courser, Keyword.HASTE) shouldBe true
    }

    test("equipped creature: accepting destroys all attached Equipment") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)

        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val sword1 = driver.putPermanentOnBattlefield(opp, "Test Sword")
        val sword2 = driver.putPermanentOnBattlefield(opp, "Test Sword")
        val loose = driver.putPermanentOnBattlefield(opp, "Test Sword")
        driver.attachEquipment(sword1, courser)
        driver.attachEquipment(sword2, courser)

        driver.cast(you, courser)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe you
        driver.submitYesNo(you, true)

        driver.state.getBattlefield().contains(sword1) shouldBe false
        driver.state.getBattlefield().contains(sword2) shouldBe false
        driver.state.getBattlefield().contains(loose) shouldBe true
        driver.state.projectedState.getController(courser) shouldBe you
        driver.state.projectedState.hasKeyword(courser, Keyword.HASTE) shouldBe true
    }

    test("equipped creature: declining keeps the Equipment") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)

        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val sword = driver.putPermanentOnBattlefield(opp, "Test Sword")
        driver.attachEquipment(sword, courser)

        driver.cast(you, courser)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(you, false)

        driver.state.getBattlefield().contains(sword) shouldBe true
        driver.state.getEntity(sword)?.get<AttachedToComponent>() shouldNotBe null
        driver.state.projectedState.getController(courser) shouldBe you
    }
})
