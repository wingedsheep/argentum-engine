package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for the "whenever one or more [filter] block" batch trigger —
 * `EventPattern.BlockEvent(batch = true)`, spelled `Triggers.oneOrMore(filter).block()`.
 *
 * CR 603.2c: an ability that triggers on "one or more" objects doing something triggers once per
 * event, however many objects took part. A block declaration is one event, so two blockers fire
 * the batch form once where the singular "whenever a creature blocks" fires once per blocker, and
 * a declaration with no (matching) blocker fires nothing — Tide of War's ruling: "triggers only
 * once each combat and only if at least one blocker is declared."
 */
class BlockBatchTriggerTest : FunSpec({

    val anyBlockBatch = card("Any Block Batch Observer") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.oneOrMore(GameObjectFilter.Creature).block()
            effect = Effects.GainLife(1)
        }
    }

    val yourBlockBatch = card("Your Block Batch Observer") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.oneOrMore(GameObjectFilter.Creature.youControl()).block()
            effect = Effects.GainLife(1)
        }
    }

    val perBlockerObserver = card("Per Blocker Observer") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature).blocks()
            effect = Effects.GainLife(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(anyBlockBatch, yourBlockBatch, perBlockerObserver))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Attack with [attackers] and declare [blocks] (blocker → attacker) for the defender. */
    fun attackAndBlock(driver: GameTestDriver, attackers: List<EntityId>, blocks: Map<EntityId, EntityId>) {
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        (attackers + blocks.keys).forEach(driver::removeSummoningSickness)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, attackers, defender)
        driver.bothPass()
        driver.declareBlockers(defender, blocks.mapValues { listOf(it.value) })
    }

    test("two blockers fire the batch form once, the singular form once per blocker") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(active, "Any Block Batch Observer")
        val a1 = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val a2 = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val b1 = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        val b2 = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        attackAndBlock(driver, listOf(a1, a2), mapOf(b1 to a1, b2 to a2))
        driver.stackSize shouldBe 1

        val driver2 = createDriver()
        val active2 = driver2.activePlayer!!
        val defender2 = driver2.getOpponent(active2)
        driver2.putPermanentOnBattlefield(active2, "Per Blocker Observer")
        val c1 = driver2.putCreatureOnBattlefield(active2, "Grizzly Bears")
        val c2 = driver2.putCreatureOnBattlefield(active2, "Grizzly Bears")
        val d1 = driver2.putCreatureOnBattlefield(defender2, "Grizzly Bears")
        val d2 = driver2.putCreatureOnBattlefield(defender2, "Grizzly Bears")

        attackAndBlock(driver2, listOf(c1, c2), mapOf(d1 to c1, d2 to c2))
        driver2.stackSize shouldBe 2
    }

    test("two creatures blocking the same attacker still fire the batch form once") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(defender, "Any Block Batch Observer")
        val attacker = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val b1 = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        val b2 = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        attackAndBlock(driver, listOf(attacker), mapOf(b1 to attacker, b2 to attacker))
        driver.stackSize shouldBe 1
    }

    test("no blockers declared fires nothing") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.putPermanentOnBattlefield(active, "Any Block Batch Observer")
        val attacker = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.putCreatureOnBattlefield(driver.getOpponent(active), "Grizzly Bears")

        attackAndBlock(driver, listOf(attacker), emptyMap())
        driver.stackSize shouldBe 0
    }

    test("the filter is read against the blockers: 'creatures you control' needs one of yours to block") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        // The attacker's observer: none of its controller's creatures block.
        driver.putPermanentOnBattlefield(active, "Your Block Batch Observer")
        val attacker = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        val blocker = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        attackAndBlock(driver, listOf(attacker), mapOf(blocker to attacker))
        driver.stackSize shouldBe 0

        val driver2 = createDriver()
        val active2 = driver2.activePlayer!!
        val defender2 = driver2.getOpponent(active2)
        // The defender's observer: two of its creatures block, one trigger.
        driver2.putPermanentOnBattlefield(defender2, "Your Block Batch Observer")
        val attacker2 = driver2.putCreatureOnBattlefield(active2, "Grizzly Bears")
        val b1 = driver2.putCreatureOnBattlefield(defender2, "Grizzly Bears")
        val b2 = driver2.putCreatureOnBattlefield(defender2, "Grizzly Bears")

        attackAndBlock(driver2, listOf(attacker2), mapOf(b1 to attacker2, b2 to attacker2))
        driver2.stackSize shouldBe 1
    }

    test("description reads as the batch wording and oneOrMoreOther is rejected") {
        EventPattern.BlockEvent(batch = true).description shouldBe "one or more creatures block"
        shouldThrow<IllegalArgumentException> {
            Triggers.oneOrMoreOther(GameObjectFilter.Creature).block()
        }
    }

    test("two defending players each blocking still fire the batch form once (CR 802.4)") {
        val registry = CardRegistry().also { r -> TestCards.all.forEach(r::register); r.register(anyBlockBatch) }
        val init = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..3).map { PlayerConfig("Player $it", Deck.of("Mountain" to 40), 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        val (a, b, c) = init.playerIds
        var state = init.state

        fun put(def: CardDefinition, owner: EntityId, attacking: EntityId? = null): EntityId {
            val id = EntityId.generate()
            var container = ComponentContainer.of(
                CardComponent(
                    cardDefinitionId = def.name, name = def.name, manaCost = def.manaCost,
                    typeLine = def.typeLine, baseStats = def.creatureStats, ownerId = owner
                ),
                OwnerComponent(owner),
                ControllerComponent(owner)
            )
            if (attacking != null) container = container.with(AttackingComponent(defenderId = attacking))
            state = state.withEntity(id, container).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id)
            return id
        }

        val bears = registry.requireCard("Grizzly Bears")
        put(anyBlockBatch, a)
        val atkB = put(bears, a, attacking = b)
        val atkC = put(bears, a, attacking = c)
        val blkB = put(bears, b)
        val blkC = put(bears, c)
        state = state.updateEntity(a) { it.with(AttackersDeclaredThisCombatComponent) }
            .copy(step = Step.DECLARE_BLOCKERS, phase = Phase.COMBAT)
            .withPriority(b)

        val processor = ActionProcessor(registry)
        state = processor.process(state, DeclareBlockers(b, mapOf(blkB to listOf(atkB)))).result.newState
        state = processor.process(state, DeclareBlockers(c, mapOf(blkC to listOf(atkC)))).result.newState

        (state.stack.size + state.pendingTriggers.size) shouldBe 1
    }
})
