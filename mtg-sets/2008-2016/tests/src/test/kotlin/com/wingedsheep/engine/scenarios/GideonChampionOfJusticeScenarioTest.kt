package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.GideonChampionOfJustice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Gideon, Champion of Justice {2}{W}{W} — Legendary Planeswalker — Gideon (loyalty 4)
 *   +1: Put a loyalty counter on Gideon for each creature target opponent controls.
 *   0: Until end of turn, Gideon becomes a Human Soldier creature with P/T each equal to the number
 *      of loyalty counters on him and gains indestructible. He's still a planeswalker. Prevent all
 *      damage that would be dealt to him this turn.
 *   −15: Exile all other permanents.
 */
class GideonChampionOfJusticeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GideonChampionOfJustice))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun putGideon(driver: GameTestDriver, playerId: EntityId, loyalty: Int): EntityId {
        val gideon = driver.putPermanentOnBattlefield(playerId, "Gideon, Champion of Justice")
        driver.addComponent(gideon, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        return gideon
    }

    fun loyalty(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    val abilities = GideonChampionOfJustice.script.activatedAbilities

    test("+1 adds a loyalty counter for each creature the target opponent controls, on top of the cost") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val gideon = putGideon(driver, me, 4)
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        // My own creature is not counted.
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = gideon,
                abilityId = abilities[0].id,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        )
        loyalty(driver, gideon) shouldBe 5
        driver.bothPass()

        loyalty(driver, gideon) shouldBe 7
    }

    test("0 makes Gideon an indestructible Human Soldier planeswalker creature with P/T locked to his loyalty") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val gideon = putGideon(driver, me, 5)

        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = gideon, abilityId = abilities[1].id)
        )
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.isCreature(gideon) shouldBe true
        projected.isPlaneswalker(gideon) shouldBe true
        projected.getSubtypes(gideon) shouldContainAll setOf("Human", "Soldier", "Gideon")
        projected.hasKeyword(gideon, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.getPower(gideon) shouldBe 5
        projected.getToughness(gideon) shouldBe 5

        // Loyalty changing later in the turn doesn't move the locked P/T.
        driver.addComponent(gideon, CountersComponent(mapOf(CounterType.LOYALTY to 9)))
        driver.state.projectedState.getPower(gideon) shouldBe 5
        driver.state.projectedState.getToughness(gideon) shouldBe 5
    }

    test("0 prevents all damage that would be dealt to Gideon this turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val gideon = putGideon(driver, me, 4)

        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = gideon, abilityId = abilities[1].id)
        )
        driver.bothPass()

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(gideon))
        driver.bothPass()

        driver.getGraveyardCardNames(me) shouldContain "Lightning Bolt"
        loyalty(driver, gideon) shouldBe 4
        driver.findPermanent(me, "Gideon, Champion of Justice") shouldBe gideon
    }

    test("−15 exiles every other permanent on both sides") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val gideon = putGideon(driver, me, 15)
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putPermanentOnBattlefield(opponent, "Plains")

        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = gideon, abilityId = abilities[2].id)
        )
        driver.bothPass()

        driver.state.getBattlefield().filter { it != gideon } shouldBe emptyList()
        driver.getExileCardNames(me) shouldContain "Grizzly Bears"
        driver.getExileCardNames(opponent) shouldContainAll listOf("Grizzly Bears", "Plains")
        // Gideon himself is not exiled; at 0 loyalty he goes to the graveyard instead.
        driver.getGraveyardCardNames(me) shouldContain "Gideon, Champion of Justice"
    }
})
