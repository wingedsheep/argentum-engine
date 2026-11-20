package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.ScourgeOfSkolaVale
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scourge of Skola Vale (BNG #137) — {2}{G} 0/0 Hydra with trample.
 *
 *   This creature enters with two +1/+1 counters on it.
 *   {T}, Sacrifice another creature: Put a number of +1/+1 counters on this creature equal to
 *   the sacrificed creature's toughness.
 *
 * Pins the cost-linked amount: the counters equal the sacrificed creature's toughness as it last
 * existed on the battlefield (a Giant Growth pump counts), and the Hydra can't sacrifice itself.
 */
class ScourgeOfSkolaValeScenarioTest : FunSpec({

    fun plusOnes(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun castScourge(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(player, "Scourge of Skola Vale")
        driver.giveMana(player, Color.GREEN, 3)
        driver.castSpell(player, card)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val scourge = driver.findPermanent(player, "Scourge of Skola Vale").shouldNotBeNull()
        driver.removeSummoningSickness(scourge)
        return Triple(driver, player, scourge)
    }

    fun activate(player: EntityId, scourge: EntityId, fodder: EntityId) =
        ActivateAbility(
            playerId = player,
            sourceId = scourge,
            abilityId = ScourgeOfSkolaVale.activatedAbilities.single().id,
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
        )

    test("enters with two +1/+1 counters") {
        val (driver, _, scourge) = castScourge()

        plusOnes(driver, scourge) shouldBe 2
        driver.state.projectedState.getPower(scourge) shouldBe 2
        driver.state.projectedState.getToughness(scourge) shouldBe 2
    }

    test("counters equal the sacrificed creature's last-known toughness, pumps included") {
        val (driver, player, scourge) = castScourge()
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val growth = driver.putCardInHand(player, "Giant Growth")
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, growth, listOf(bears))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.state.projectedState.getToughness(bears) shouldBe 5

        driver.submitSuccess(activate(player, scourge, bears))
        driver.getGraveyard(player) shouldContain bears
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        plusOnes(driver, scourge) shouldBe 7
        driver.state.projectedState.getPower(scourge) shouldBe 7
        driver.state.projectedState.getToughness(scourge) shouldBe 7
    }

    test("can't sacrifice itself to its own ability") {
        val (driver, player, scourge) = castScourge()

        driver.submitExpectFailure(activate(player, scourge, scourge))

        driver.findPermanent(player, "Scourge of Skola Vale").shouldNotBeNull()
        plusOnes(driver, scourge) shouldBe 2
    }
})
