package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.mrd.cards.LeoninScimitar
import com.wingedsheep.mtg.sets.definitions.znr.cards.AkiriFearlessVoyager
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Akiri, Fearless Voyager — {1}{R}{W} 3/3
 *   "Whenever you attack a player with one or more equipped creatures, draw a card.
 *    {W}: You may unattach an Equipment from a creature you control. If you do, tap that creature
 *    and it gains indestructible until end of turn."
 *
 * Pins the parts a reader can't confirm from the script: the attack trigger counts only equipped
 * attackers (one draw, however many are equipped), and the resolution-time pipeline unattaches
 * just the chosen Equipment, then taps and protects the creature it came off — or does nothing
 * when the "you may" is declined.
 */
class AkiriFearlessVoyagerScenarioTest : FunSpec({

    fun GameTestDriver.attach(equipmentId: EntityId, creatureId: EntityId) {
        var newState = state.updateEntity(equipmentId) { c -> c.with(AttachedToComponent(creatureId)) }
        val existing = newState.getEntity(creatureId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(creatureId) { c -> c.with(AttachmentsComponent(existing + equipmentId)) }
        replaceState(newState)
    }

    fun setUp(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AkiriFearlessVoyager, Bonesplitter, LeoninScimitar))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to you
    }

    fun GameTestDriver.activateAkiri(you: EntityId, akiri: EntityId) {
        giveMana(you, Color.WHITE, 1)
        submit(ActivateAbility(you, akiri, AkiriFearlessVoyager.activatedAbilities.first().id))
            .outcome shouldBe Outcome.Done
        bothPass()
    }

    test("unattaches only the chosen Equipment, then taps the creature and makes it indestructible") {
        val (driver, you) = setUp()
        val akiri = driver.putCreatureOnBattlefield(you, "Akiri, Fearless Voyager")
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val splitter = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        val scimitar = driver.putPermanentOnBattlefield(you, "Leonin Scimitar")
        driver.attach(splitter, courser)
        driver.attach(scimitar, courser)

        driver.activateAkiri(you, akiri)

        // First the creature (optional — "you may"), then which of its Equipment.
        (driver.pendingDecision as SelectCardsDecision).minSelections shouldBe 0
        driver.submitCardSelection(you, listOf(courser))
        (driver.pendingDecision as SelectCardsDecision).options.toSet() shouldBe setOf(splitter, scimitar)
        driver.submitCardSelection(you, listOf(splitter))

        driver.state.getEntity(splitter)?.get<AttachedToComponent>().shouldBeNull()
        driver.findPermanent(you, "Bonesplitter") shouldBe splitter
        driver.state.getEntity(scimitar)?.get<AttachedToComponent>()?.targetId shouldBe courser
        driver.isTapped(courser) shouldBe true
        driver.state.projectedState.hasKeyword(courser, Keyword.INDESTRUCTIBLE) shouldBe true
    }

    test("declining the choice leaves everything as it was") {
        val (driver, you) = setUp()
        val akiri = driver.putCreatureOnBattlefield(you, "Akiri, Fearless Voyager")
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val splitter = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        driver.attach(splitter, courser)

        driver.activateAkiri(you, akiri)
        driver.submitCardSelection(you, emptyList())

        driver.state.getEntity(splitter)?.get<AttachedToComponent>()?.targetId shouldBe courser
        driver.isTapped(courser) shouldBe false
        driver.state.projectedState.hasKeyword(courser, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("attacking a player with equipped creatures draws exactly one card") {
        val (driver, you) = setUp()
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, "Akiri, Fearless Voyager")
        val first = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val second = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.removeSummoningSickness(first)
        driver.removeSummoningSickness(second)
        driver.attach(driver.putPermanentOnBattlefield(you, "Bonesplitter"), first)
        driver.attach(driver.putPermanentOnBattlefield(you, "Leonin Scimitar"), second)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = driver.getHandSize(you)
        driver.declareAttackers(you, listOf(first, second), opponent).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getHandSize(you) shouldBe handBefore + 1
    }

    test("attacking with only unequipped creatures draws nothing") {
        val (driver, you) = setUp()
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, "Akiri, Fearless Voyager")
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.removeSummoningSickness(courser)
        driver.putPermanentOnBattlefield(you, "Bonesplitter") // on the battlefield, but unattached

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = driver.getHandSize(you)
        driver.declareAttackers(you, listOf(courser), opponent).outcome shouldBe Outcome.Done

        driver.stackSize shouldBe 0
        driver.getHandSize(you) shouldBe handBefore
    }
})
