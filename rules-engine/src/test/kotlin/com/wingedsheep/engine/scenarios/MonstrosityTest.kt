package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.BecameMonstrousEvent
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.MonstrousComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mechanic-level tests for the monstrosity keyword action (CR 701.37), authored as
 * `Effects.Monstrosity(n)` / `Effects.Monstrosity(DynamicAmount.XValue)`.
 *
 * The spec being pinned, clause by clause:
 *  - **701.37a** — "If this permanent isn't monstrous, put N +1/+1 counters on it and it becomes
 *    monstrous." The check is made as the ability resolves, so a second activation — or a second
 *    copy already on the stack — does nothing once the first has resolved.
 *  - **701.37b** — monstrous is a designation that lasts until the permanent leaves the
 *    battlefield; it is not an ability, so losing all abilities doesn't remove it; only permanents
 *    can become monstrous, so an ability whose source left the battlefield does nothing.
 *  - Monstrosity X with X = 0 still makes the permanent monstrous (the designation doesn't depend
 *    on any counters being placed).
 *  - "When this creature becomes monstrous" fires exactly once.
 */
class MonstrosityTest : FunSpec({

    /** `{1}: Monstrosity 2.` plus the "as long as it's monstrous" payoff shape. */
    val brute = card("Test Monstrous Brute") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        oracleText = "{1}: Monstrosity 2.\nAs long as this creature is monstrous, it has trample."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.Monstrosity(2)
        }
        staticAbility {
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source())
            condition = Conditions.SourceIsMonstrous
        }
    }

    /** `{X}: Monstrosity X.` — the Domesticated Hydra shape. */
    val hydra = card("Test Monstrous Hydra") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Hydra"
        power = 1
        toughness = 1
        oracleText = "{X}: Monstrosity X."
        activatedAbility {
            cost = Costs.Mana("{X}")
            effect = Effects.Monstrosity(DynamicAmount.XValue)
        }
    }

    /** A "when this creature becomes monstrous" payoff. */
    val swallower = card("Test Monstrous Swallower") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Elemental"
        power = 2
        toughness = 2
        oracleText = "{1}: Monstrosity 1.\nWhen this creature becomes monstrous, you gain 3 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.Monstrosity(1)
        }
        triggeredAbility {
            trigger = Triggers.self.becomesMonstrous()
            effect = Effects.GainLife(3)
        }
    }

    val recall = card("Test Monstrous Recall") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Return target creature to its owner's hand."
        spell {
            val victim = target(TargetFilter.Creature)
            effect = Effects.ReturnToHand(victim)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(brute, hydra, swallower, recall))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, perm: EntityId): Int =
        driver.state.getEntity(perm)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun isMonstrous(driver: GameTestDriver, perm: EntityId): Boolean =
        driver.state.getEntity(perm)?.has<MonstrousComponent>() == true

    fun activate(driver: GameTestDriver, player: EntityId, perm: EntityId, xValue: Int? = null) {
        val abilityId = driver.state.getEntity(perm)!!
            .get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!
            .let { driver.cardRegistry.getCard(it.name)!!.activatedAbilities.first().id }
        driver.giveMana(player, Color.GREEN, xValue ?: 1)
        driver.submit(ActivateAbility(player, perm, abilityId, xValue = xValue)).outcome shouldBe Outcome.Done
    }

    fun setUp(name: String): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val perm = driver.putCreatureOnBattlefield(player, name)
        return Triple(driver, player, perm)
    }

    test("CR 701.37a — monstrosity N puts N +1/+1 counters on and the permanent becomes monstrous") {
        val (driver, player, brute) = setUp("Test Monstrous Brute")
        driver.state.projectedState.hasKeyword(brute, Keyword.TRAMPLE) shouldBe false

        activate(driver, player, brute)
        driver.bothPass()

        plusOneCounters(driver, brute) shouldBe 2
        isMonstrous(driver, brute) shouldBe true
        driver.state.projectedState.getPower(brute) shouldBe 4
        driver.state.projectedState.hasKeyword(brute, Keyword.TRAMPLE) shouldBe true
    }

    test("CR 701.37a — activating again once monstrous does nothing") {
        val (driver, player, brute) = setUp("Test Monstrous Brute")
        activate(driver, player, brute)
        driver.bothPass()

        activate(driver, player, brute)
        driver.bothPass()

        plusOneCounters(driver, brute) shouldBe 2
    }

    test("CR 701.37a — the check is at resolution: of two activations on the stack, only the first does anything") {
        val (driver, player, brute) = setUp("Test Monstrous Brute")
        activate(driver, player, brute)
        activate(driver, player, brute)
        driver.state.stack.size shouldBe 2

        driver.bothPass()
        driver.bothPass()

        driver.state.stack.size shouldBe 0
        plusOneCounters(driver, brute) shouldBe 2
        isMonstrous(driver, brute) shouldBe true
    }

    test("monstrosity X reads the X paid in the cost") {
        val (driver, player, hydra) = setUp("Test Monstrous Hydra")
        activate(driver, player, hydra, xValue = 3)
        driver.bothPass()

        plusOneCounters(driver, hydra) shouldBe 3
        isMonstrous(driver, hydra) shouldBe true
    }

    test("monstrosity X with X = 0 still makes the permanent monstrous") {
        val (driver, player, hydra) = setUp("Test Monstrous Hydra")
        val abilityId = driver.cardRegistry.getCard("Test Monstrous Hydra")!!.activatedAbilities.first().id
        driver.submit(ActivateAbility(player, hydra, abilityId, xValue = 0)).outcome shouldBe Outcome.Done
        driver.bothPass()

        plusOneCounters(driver, hydra) shouldBe 0
        isMonstrous(driver, hydra) shouldBe true

        // ...and a later, bigger X finds it already monstrous.
        activate(driver, player, hydra, xValue = 4)
        driver.bothPass()
        plusOneCounters(driver, hydra) shouldBe 0
    }

    test("\"when this creature becomes monstrous\" triggers once, not on a later no-op activation") {
        val (driver, player, swallower) = setUp("Test Monstrous Swallower")
        val startingLife = driver.getLifeTotal(player)

        activate(driver, player, swallower)
        val result = driver.bothPass()
        result.events.filterIsInstance<BecameMonstrousEvent>().map { it.entityId } shouldBe listOf(swallower)
        // The trigger is now on the stack; resolve it.
        driver.bothPass()
        driver.getLifeTotal(player) shouldBe startingLife + 3

        activate(driver, player, swallower)
        driver.bothPass()
        driver.state.stack.size shouldBe 0
        driver.getLifeTotal(player) shouldBe startingLife + 3
    }

    test("CR 701.37b — only permanents become monstrous: a source bounced in response gets nothing") {
        val (driver, player, brute) = setUp("Test Monstrous Brute")
        activate(driver, player, brute)

        driver.giveMana(player, Color.BLUE, 1)
        val recallCard = driver.putCardInHand(player, "Test Monstrous Recall")
        driver.submit(CastSpell(player, recallCard, listOf(ChosenTarget.Permanent(brute)))).outcome shouldBe Outcome.Done
        driver.bothPass() // Recall resolves
        val result = driver.bothPass() // monstrosity resolves with its source gone

        result.events.filterIsInstance<BecameMonstrousEvent>() shouldBe emptyList()
        driver.state.getBattlefield().contains(brute) shouldBe false
        driver.state.getHand(player).forEach { plusOneCounters(driver, it) shouldBe 0 }
    }

    test("CR 701.37b — the designation ends when the permanent leaves the battlefield") {
        val (driver, player, brute) = setUp("Test Monstrous Brute")
        activate(driver, player, brute)
        driver.bothPass()
        isMonstrous(driver, brute) shouldBe true

        driver.giveMana(player, Color.BLUE, 1)
        val recallCard = driver.putCardInHand(player, "Test Monstrous Recall")
        driver.submit(CastSpell(player, recallCard, listOf(ChosenTarget.Permanent(brute)))).outcome shouldBe Outcome.Done
        driver.bothPass()

        val inHand = driver.state.getHand(player).single { id ->
            driver.state.getEntity(id)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Test Monstrous Brute"
        }
        isMonstrous(driver, inHand) shouldBe false
    }

    test("CR 701.37b — monstrous isn't an ability: losing all abilities keeps the designation") {
        val (driver, player, brute) = setUp("Test Monstrous Brute")
        activate(driver, player, brute)
        driver.bothPass()

        driver.replaceState(
            driver.state.addFloatingEffects(
                listOf(
                    ActiveFloatingEffect(
                        id = EntityId.generate(),
                        effect = FloatingEffectData(
                            layer = Layer.ABILITY,
                            modification = SerializableModification.RemoveAllAbilities,
                            affectedEntities = setOf(brute),
                        ),
                        duration = Duration.Permanent,
                        sourceId = brute,
                        sourceName = "Test Monstrous Brute",
                        controllerId = player,
                        timestamp = driver.state.timestamp,
                    )
                )
            )
        )

        isMonstrous(driver, brute) shouldBe true
        plusOneCounters(driver, brute) shouldBe 2
    }
})
