package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.GleamOfAuthority
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gleam of Authority — {1}{W} Aura.
 * Enchanted creature gets +1/+1 for each +1/+1 counter on other creatures you control.
 * Enchanted creature has vigilance and "{W}, {T}: Bolster 1."
 *
 * Proves the bonus sums *counters* (not creatures with counters), that "other" excludes the
 * enchanted creature rather than the Aura, that "you" is the Aura's controller, and that the
 * granted bolster ability is activatable from the enchanted creature.
 */
class GleamOfAuthorityScenarioTest : FunSpec({

    val projector = StateProjector()

    val grantedAbilityId =
        GleamOfAuthority.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.id

    val squire = CardDefinition.creature(
        name = "Test Squire",
        manaCost = ManaCost.parse("{W}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GleamOfAuthority, squire))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.enchant(controller: EntityId, host: EntityId): EntityId {
        val aura = putPermanentOnBattlefield(controller, "Gleam of Authority")
        var s = state.updateEntity(aura) { it.with(AttachedToComponent(host)) }
        s = s.updateEntity(host) { it.with(AttachmentsComponent(listOf(aura))) }
        replaceState(s)
        return aura
    }

    fun GameTestDriver.addCounters(entityId: EntityId, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        })
    }

    fun GameTestDriver.plusOneCounters(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("gets +1/+1 per +1/+1 counter on other creatures you control and has vigilance") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other2 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.enchant(me, bears)

        projector.project(driver.state).let {
            it.getPower(bears) shouldBe 2
            it.getToughness(bears) shouldBe 2
            it.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        }

        // Counts counters, not creatures: 2 + 1 counters on two other creatures = +3/+3.
        driver.addCounters(other1, 2)
        driver.addCounters(other2, 1)
        projector.project(driver.state).let {
            it.getPower(bears) shouldBe 5
            it.getToughness(bears) shouldBe 5
        }

        // A counter on the enchanted creature itself only counts as its own +1/+1.
        driver.addCounters(bears, 1)
        projector.project(driver.state).let {
            it.getPower(bears) shouldBe 6
            it.getToughness(bears) shouldBe 6
        }
    }

    test("counts the Aura controller's creatures, not the enchanted creature's controller's") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val theirOther = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val mine = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.enchant(me, theirBears)

        driver.addCounters(theirOther, 3)
        driver.addCounters(mine, 2)

        projector.project(driver.state).let {
            it.getPower(theirBears) shouldBe 4
            it.getToughness(theirBears) shouldBe 4
        }
    }

    test("the granted {W}, {T} ability taps the enchanted creature and bolsters 1") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        val smallest = driver.putCreatureOnBattlefield(me, "Test Squire")
        driver.enchant(me, bears)

        driver.giveMana(me, Color.WHITE, 1)
        driver.submit(
            ActivateAbility(playerId = me, sourceId = bears, abilityId = grantedAbilityId)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.isTapped(bears) shouldBe true
        // The 1/1 is the unique least-toughness creature, so bolster lands on it...
        driver.plusOneCounters(smallest) shouldBe 1
        driver.plusOneCounters(bears) shouldBe 0
        // ...and that new counter on another creature feeds the enchanted creature's bonus.
        projector.project(driver.state).let {
            it.getPower(bears) shouldBe 3
            it.getToughness(bears) shouldBe 3
        }
    }
})
