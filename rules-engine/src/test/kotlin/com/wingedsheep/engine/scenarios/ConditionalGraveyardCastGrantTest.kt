package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * A `MayCastFromGraveyard` grant wrapped in a condition (`staticAbility { condition = … }`) — the
 * shape of a Spacecraft's station threshold block (Exploration Broodship's 8+). Pins that both the
 * enumerator and the cast handler resolve the condition against the granting permanent: no action
 * and a rejected cast while it fails, an ordinary graveyard cast once it holds.
 */
class ConditionalGraveyardCastGrantTest : FunSpec({

    val bear = card("Crypt Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val gatedBench = card("Charged Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            condition = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 2)
            ability = MayCastFromGraveyard(GameObjectFilter.Creature)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(bear, gatedBench))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.charge(id: EntityId, count: Int) {
        replaceState(state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.CHARGE, count))
        })
    }

    fun GameTestDriver.bearCasts(player: EntityId, bearId: EntityId) = legalActions(player)
        .filter { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == bearId }

    test("the grant offers nothing and the handler rejects the cast while the condition fails") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val bench = driver.putPermanentOnBattlefield(player, "Charged Bench")
        driver.charge(bench, 1)
        val bearId = driver.putCardInGraveyard(player, "Crypt Bear")
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)

        driver.bearCasts(player, bearId).shouldBeEmpty()
        driver.submitExpectFailure(CastSpell(player, bearId))
        driver.getGraveyard(player).contains(bearId) shouldBe true
    }

    test("once the condition holds the card is cast from the graveyard") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val bench = driver.putPermanentOnBattlefield(player, "Charged Bench")
        driver.charge(bench, 2)
        val bearId = driver.putCardInGraveyard(player, "Crypt Bear")
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)

        val cast = driver.bearCasts(player, bearId)
        cast shouldHaveSize 1
        driver.submit(cast.single().action).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.getGraveyard(player).contains(bearId) shouldBe false
        driver.state.getBattlefield().contains(bearId) shouldBe true
    }
})
