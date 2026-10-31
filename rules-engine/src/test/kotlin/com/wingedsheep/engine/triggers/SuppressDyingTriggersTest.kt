package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SuppressZoneChangeTriggers
import com.wingedsheep.sdk.scripting.ZoneChangeTriggerCause
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The dying half of [SuppressZoneChangeTriggers] (Hushbringer: "Creatures entering or dying don't
 * cause abilities to trigger."), pinned to Hushbringer's rulings:
 *  - a creature's own dies trigger and other permanents' dies / leaves-the-battlefield / batched
 *    "one or more creatures die" triggers are all stopped;
 *  - those abilities look back in time, so creatures dying alongside the lock — the lock itself
 *    included — still trigger nothing;
 *  - a "put into a graveyard from anywhere" ability is stopped only while the lock stays on the
 *    battlefield, and never when it is the dying card's own;
 *  - the dying object is matched by last-known information against the lock's filter, so a
 *    noncreature artifact dying still triggers;
 *  - an ability that triggers on the cause of the death ("whenever you sacrifice") still triggers.
 *
 * Every watcher gains its controller life, so each fired trigger shows up as life.
 */
class SuppressDyingTriggersTest : FunSpec({
    val both = setOf(ZoneChangeTriggerCause.ENTERING, ZoneChangeTriggerCause.DYING)
    val lockOrb = card("Test Hush Orb") {
        typeLine = "Artifact"
        staticAbility { ability = SuppressZoneChangeTriggers(causes = both) }
    }
    val lockCreature = card("Test Hushbringer") {
        typeLine = "Creature — Faerie"
        power = 1
        toughness = 2
        staticAbility { ability = SuppressZoneChangeTriggers(causes = both) }
    }
    val enteringOnlyOrb = card("Test Entering Orb") {
        typeLine = "Artifact"
        staticAbility { ability = SuppressZoneChangeTriggers() }
    }
    val diesWatcher = card("Test Dies Watcher") {
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature).dies()
            effect = Effects.GainLife(1)
        }
    }
    val leavesWatcher = card("Test Leaves Watcher") {
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature).leaves()
            effect = Effects.GainLife(1)
        }
    }
    val batchWatcher = card("Test Batch Dies Watcher") {
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.oneOrMore(GameObjectFilter.Creature.youControl()).die()
            effect = Effects.GainLife(1)
        }
    }
    val anywhereWatcher = card("Test From Anywhere Watcher") {
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature).changesZone(to = Zone.GRAVEYARD)
            effect = Effects.GainLife(1)
        }
    }
    val artifactWatcher = card("Test Artifact Dies Watcher") {
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Artifact).dies()
            effect = Effects.GainLife(1)
        }
    }
    val sacrificeWatcher = card("Test Sacrifice Watcher") {
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.you.sacrifices(GameObjectFilter.Creature)
            effect = Effects.GainLife(1)
        }
    }
    val selfDies = card("Test Self Dies Creature") {
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.dies()
            effect = Effects.GainLife(5)
        }
    }
    val selfAnywhere = card("Test Self Anywhere Creature") {
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        triggeredAbility {
            triggerZone = Zone.GRAVEYARD
            trigger = Triggers.self.changesZone(to = Zone.GRAVEYARD)
            effect = Effects.GainLife(5)
        }
    }
    val enterer = card("Test Enters Creature") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(5)
        }
    }
    val plainArtifact = card("Test Plain Artifact") { typeLine = "Artifact" }
    val exileAll = card("Test Exile") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        spell {
            val victim = target(TargetFilter.Creature)
            effect = Effects.Exile(victim)
        }
    }
    val wrath = card("Test Wrath") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature) }
    }
    val shatterstorm = card("Test Shatterstorm") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        spell { effect = Effects.DestroyAll(GameObjectFilter.Artifact) }
    }
    val sacrificeAll = card("Test Mass Sacrifice") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        spell { effect = Effects.SacrificeAll(GameObjectFilter.Creature) }
    }

    fun driver(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all + listOf(
                lockOrb, lockCreature, enteringOnlyOrb, diesWatcher, leavesWatcher, batchWatcher,
                anywhereWatcher, artifactWatcher, sacrificeWatcher, selfDies, selfAnywhere, enterer,
                plainArtifact, wrath, shatterstorm, sacrificeAll, exileAll
            )
        )
        d.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to you
    }

    fun GameTestDriver.castAndSettle(you: EntityId, name: String) {
        val spell = putCardInHand(you, name)
        giveColorlessMana(you, if (name == enterer.name) 1 else 2)
        castSpell(you, spell).outcome shouldBe Outcome.Done
        var guard = 0
        while (!isPaused && state.stack.isNotEmpty() && guard++ < 40) bothPass()
    }

    test("control: without a lock, every death-caused watcher triggers") {
        val (d, you) = driver()
        listOf(diesWatcher, leavesWatcher, batchWatcher).forEach { d.putPermanentOnBattlefield(you, it.name) }
        d.putCreatureOnBattlefield(you, selfDies.name)
        d.putCreatureOnBattlefield(you, "Centaur Courser")
        d.castAndSettle(you, wrath.name)
        withClue("dies 2 + leaves 2 + batch 1 + own dies trigger 5") { d.getLifeTotal(you) shouldBe 30 }
    }

    test("with the lock out, creatures dying trigger nothing — own, observer, leaves and batch alike") {
        val (d, you) = driver()
        d.putPermanentOnBattlefield(you, lockOrb.name)
        listOf(diesWatcher, leavesWatcher, batchWatcher).forEach { d.putPermanentOnBattlefield(you, it.name) }
        d.putCreatureOnBattlefield(you, selfDies.name)
        d.putCreatureOnBattlefield(you, "Centaur Courser")
        d.castAndSettle(you, wrath.name)
        withClue("no trigger fired") { d.getLifeTotal(you) shouldBe 20 }
        withClue("the creatures still died") { d.findPermanent(you, "Centaur Courser") shouldBe null }
    }

    test("an entering-only lock leaves deaths alone") {
        val (d, you) = driver()
        d.putPermanentOnBattlefield(you, enteringOnlyOrb.name)
        d.putPermanentOnBattlefield(you, diesWatcher.name)
        d.putCreatureOnBattlefield(you, "Centaur Courser")
        d.castAndSettle(you, wrath.name)
        d.getLifeTotal(you) shouldBe 21
    }

    test("a lock covering both causes still stops enters triggers") {
        val (d, you) = driver()
        d.putPermanentOnBattlefield(you, lockOrb.name)
        d.castAndSettle(you, enterer.name)
        d.getLifeTotal(you) shouldBe 20
    }

    test("look-back: creatures dying at the same time as the lock, and the lock itself, trigger nothing") {
        val (d, you) = driver()
        listOf(diesWatcher, leavesWatcher, batchWatcher).forEach { d.putPermanentOnBattlefield(you, it.name) }
        d.putCreatureOnBattlefield(you, lockCreature.name)
        d.putCreatureOnBattlefield(you, selfDies.name)
        d.castAndSettle(you, wrath.name)
        withClue("the lock's own death and the others' are all suppressed") { d.getLifeTotal(you) shouldBe 20 }
        d.findPermanent(you, lockCreature.name) shouldBe null
    }

    test("from anywhere: stopped while the lock stays on the battlefield") {
        val (d, you) = driver()
        d.putPermanentOnBattlefield(you, lockOrb.name)
        d.putPermanentOnBattlefield(you, anywhereWatcher.name)
        d.putCreatureOnBattlefield(you, "Centaur Courser")
        d.castAndSettle(you, wrath.name)
        d.getLifeTotal(you) shouldBe 20
    }

    test("from anywhere: not stopped when the lock leaves at the same time — no look-back") {
        val (d, you) = driver()
        d.putPermanentOnBattlefield(you, anywhereWatcher.name)
        d.putCreatureOnBattlefield(you, lockCreature.name)
        d.putCreatureOnBattlefield(you, "Centaur Courser")
        d.castAndSettle(you, wrath.name)
        withClue("one trigger each for the Courser and the lock") { d.getLifeTotal(you) shouldBe 22 }
    }

    test("from anywhere: the dying card's own ability still triggers under the lock") {
        val (d, you) = driver()
        d.putPermanentOnBattlefield(you, lockOrb.name)
        d.putCreatureOnBattlefield(you, selfAnywhere.name)
        d.castAndSettle(you, wrath.name)
        d.getLifeTotal(you) shouldBe 25
    }

    test("a noncreature artifact dying is outside the creature filter and still triggers") {
        val (d, you) = driver()
        d.putPermanentOnBattlefield(you, lockOrb.name)
        d.putPermanentOnBattlefield(you, artifactWatcher.name)
        d.putPermanentOnBattlefield(you, plainArtifact.name)
        d.castAndSettle(you, shatterstorm.name)
        withClue("the plain artifact and the (noncreature) lock both died and both triggered") {
            d.getLifeTotal(you) shouldBe 22
        }
    }

    test("a creature leaving without dying still triggers leaves-the-battlefield abilities") {
        val (d, you) = driver()
        d.putPermanentOnBattlefield(you, lockOrb.name)
        d.putPermanentOnBattlefield(you, leavesWatcher.name)
        val courser = d.putCreatureOnBattlefield(you, "Centaur Courser")
        val spell = d.putCardInHand(you, exileAll.name)
        d.giveColorlessMana(you, 2)
        d.castSpell(you, spell, listOf(courser)).outcome shouldBe Outcome.Done
        var guard = 0
        while (!d.isPaused && d.state.stack.isNotEmpty() && guard++ < 40) d.bothPass()
        d.getLifeTotal(you) shouldBe 21
    }

    test("a trigger on the cause of the death — a sacrifice — still triggers") {
        val (d, you) = driver()
        d.putPermanentOnBattlefield(you, lockOrb.name)
        d.putPermanentOnBattlefield(you, sacrificeWatcher.name)
        d.putPermanentOnBattlefield(you, diesWatcher.name)
        d.putCreatureOnBattlefield(you, "Centaur Courser")
        d.castAndSettle(you, sacrificeAll.name)
        withClue("the sacrifice trigger fires, the dies trigger doesn't") { d.getLifeTotal(you) shouldBe 21 }
    }
})
