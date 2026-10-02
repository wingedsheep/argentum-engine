package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Enchanted creature has 'When this creature dies, …'" — a triggered ability an Aura grants its
 * host. Leaves-the-battlefield abilities look back in time (CR 603.10a): whether the host had the
 * granted ability is decided by the state immediately before it left. By trigger time the Aura may
 * be gone too — put into the graveyard by the state-based action that follows its host's death
 * (CR 704.5m), destroyed in the same event, or (bestow) turned back into a creature — so the
 * engine freezes the host's attachment-granted triggers on its exit snapshot.
 */
class AttachmentGrantLookBackTest : FunSpec({

    // "Enchanted creature has 'When this creature dies, you gain 3 life.'"
    val mark = card("Look-Back Mark") {
        manaCost = "{0}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        staticAbility {
            ability = GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.dies()
                    effect = Effects.GainLife(3)
                },
            )
        }
    }

    val host = card("Look-Back Host") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val kill = card("Look-Back Kill") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Creature))
            effect = Effects.Destroy(t)
        }
    }

    // Lethal damage: the host dies to the state-based action, its Aura to the next one.
    val burn = card("Look-Back Burn") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Creature))
            effect = Effects.DealDamage(5, t)
        }
    }

    // One event, the Aura first: the host's exit sees no attachment unless the look-back is batch-wide.
    val wipe = card("Look-Back Wipe") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Enchantment or GameObjectFilter.Creature),
                Effects.Move(EffectTarget.IterationEntity, Zone.GRAVEYARD, byDestruction = true)
            )
        }
    }

    val collectionWipe = card("Look-Back Collection Wipe") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DestroyAll(GameObjectFilter.Enchantment or GameObjectFilter.Creature) }
    }

    // Each of host and Aura is sacrificed in one event — the sacrifice loop must freeze them together.
    val sacrificeBoth = card("Look-Back Sacrifice") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Sacrifice(
                GameObjectFilter.Enchantment or GameObjectFilter.Creature,
                count = 2,
                target = EffectTarget.Controller
            )
        }
    }

    val bounce = card("Look-Back Bounce") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Creature))
            effect = Effects.Move(t, Zone.HAND)
        }
    }

    val disenchant = card("Look-Back Disenchant") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Enchantment))
            effect = Effects.Destroy(t)
        }
    }

    fun GameTestDriver.cast(name: String, targets: List<EntityId> = emptyList()) {
        castSpell(player1, putCardInHand(player1, name), targets).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    // Reorder the battlefield so a one-at-a-time wipe moves the Aura before its host: the host's
    // exit then sees no attachment unless the look-back was frozen before the first move.
    fun GameTestDriver.moveHostLast(hostId: EntityId) {
        val key = ZoneKey(player1, Zone.BATTLEFIELD)
        replaceState(state.removeFromZone(key, hostId).addToZone(key, hostId))
    }

    fun setup(marks: Int = 1): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(mark, host, kill, burn, wipe, collectionWipe, sacrificeBoth, bounce, disenchant))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val hostId = driver.putCreatureOnBattlefield(driver.player1, "Look-Back Host")
        repeat(marks) { driver.cast("Look-Back Mark", listOf(hostId)) }
        return driver to hostId
    }

    test("host destroyed while enchanted — the granted dies trigger fires") {
        val (driver, hostId) = setup()
        driver.cast("Look-Back Kill", listOf(hostId))
        driver.getLifeTotal(driver.player1) shouldBe 23
    }

    test("host dies to lethal damage — the Aura is gone by trigger time, the trigger still fires") {
        val (driver, hostId) = setup()
        driver.cast("Look-Back Burn", listOf(hostId))
        withClue("the Aura went to the graveyard in the SBA after its host's") {
            driver.findPermanent(driver.player1, "Look-Back Mark") shouldBe null
        }
        driver.getLifeTotal(driver.player1) shouldBe 23
    }

    test("host and Aura destroyed in one ForEachInGroup event, the Aura first — the look-back sees the attachment") {
        val (driver, hostId) = setup()
        driver.moveHostLast(hostId)
        driver.cast("Look-Back Wipe")
        driver.getLifeTotal(driver.player1) shouldBe 23
    }

    test("host and Aura destroyed in one MoveCollection batch, the Aura first — the look-back sees the attachment") {
        val (driver, hostId) = setup()
        driver.moveHostLast(hostId)
        driver.cast("Look-Back Collection Wipe")
        driver.getLifeTotal(driver.player1) shouldBe 23
    }

    test("host and Aura sacrificed in one event, the Aura first — the look-back sees the attachment") {
        val (driver, hostId) = setup()
        driver.moveHostLast(hostId)
        driver.cast("Look-Back Sacrifice")
        withClue("both left") { driver.findPermanent(driver.player1, "Look-Back Host") shouldBe null }
        driver.getLifeTotal(driver.player1) shouldBe 23
    }

    test("two Auras granting the same ability — two triggers") {
        val (driver, hostId) = setup(marks = 2)
        driver.cast("Look-Back Burn", listOf(hostId))
        driver.getLifeTotal(driver.player1) shouldBe 26
    }

    test("the Aura destroyed by an earlier, separate event — no trigger") {
        val (driver, hostId) = setup()
        driver.cast("Look-Back Disenchant", listOf(driver.findPermanent(driver.player1, "Look-Back Mark")!!))
        driver.cast("Look-Back Kill", listOf(hostId))
        driver.getLifeTotal(driver.player1) shouldBe 20
    }

    test("host returned to hand — not a death, no trigger") {
        val (driver, hostId) = setup()
        driver.cast("Look-Back Bounce", listOf(hostId))
        driver.getLifeTotal(driver.player1) shouldBe 20
    }
})
