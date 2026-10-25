package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.GrandMasterOfFlowers
import com.wingedsheep.mtg.sets.definitions.afr.cards.MonkOfTheOpenHand
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Grand Master of Flowers {2}{W}{W} — Legendary Planeswalker — Bahamut (loyalty 3)
 *   As long as Grand Master of Flowers has seven or more loyalty counters on him, he's a 7/7
 *   Dragon God creature with flying and indestructible.
 *   +1: Target creature without first strike, double strike, or vigilance can't attack or block
 *   until your next turn.
 *   +1: Search your library and/or graveyard for a card named Monk of the Open Hand, reveal it,
 *   and put it into your hand. If you search your library this way, shuffle.
 *
 * Proves the loyalty-gated animation switches on as the +1 cost is paid (ruling 2021-07-23), that
 * he stops being a planeswalker while animated, and that his loyalty abilities stay activatable
 * while he is a creature and not a planeswalker.
 */
class GrandMasterOfFlowersScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GrandMasterOfFlowers, MonkOfTheOpenHand))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun putMaster(driver: GameTestDriver, playerId: EntityId, loyalty: Int): EntityId {
        val master = driver.putPermanentOnBattlefield(playerId, "Grand Master of Flowers")
        driver.addComponent(master, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        return master
    }

    fun loyalty(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    val abilities = GrandMasterOfFlowers.script.activatedAbilities

    test("below seven loyalty he is a planeswalker, not a creature") {
        val driver = createDriver()
        val master = putMaster(driver, driver.activePlayer!!, 6)

        val projected = driver.state.projectedState
        projected.isCreature(master) shouldBe false
        projected.isPlaneswalker(master) shouldBe true
    }

    test("paying +1 from six makes him a 7/7 Dragon God before the search resolves, and the search finds Monk in the graveyard") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val master = putMaster(driver, me, 6)
        val monk = driver.putCardInGraveyard(me, "Monk of the Open Hand")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = master, abilityId = abilities[1].id))
        loyalty(driver, master) shouldBe 7

        // The ability is still on the stack, but the cost already animated him.
        val projected = driver.state.projectedState
        projected.isCreature(master) shouldBe true
        projected.isPlaneswalker(master) shouldBe false
        projected.getPower(master) shouldBe 7
        projected.getToughness(master) shouldBe 7
        projected.getSubtypes(master) shouldContainAll setOf("Dragon", "God")
        projected.hasKeyword(master, Keyword.FLYING) shouldBe true
        projected.hasKeyword(master, Keyword.INDESTRUCTIBLE) shouldBe true

        driver.bothPass()
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain monk
        driver.submitDecision(me, CardsSelectedResponse(decision.id, listOf(monk)))

        driver.findCardInHand(me, "Monk of the Open Hand") shouldBe monk
    }

    test("while animated he can still activate +1, locking a creature out of attacking and blocking") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val master = putMaster(driver, me, 7)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.state.projectedState.isPlaneswalker(master) shouldBe false

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = master,
                abilityId = abilities[0].id,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        )
        driver.bothPass()

        loyalty(driver, master) shouldBe 8
        driver.state.projectedState.cantAttack(bears) shouldBe true
        driver.state.projectedState.cantBlock(bears) shouldBe true
        driver.state.projectedState.isCreature(master) shouldBe true
    }
})
