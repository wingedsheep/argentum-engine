package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "This creature has 'When this creature dies, …' as long as <condition>" — a conditional
 * `Scope.Self` [GrantTriggeredAbility]. Leaves-the-battlefield abilities look back in time
 * (CR 603.10a): whether the creature had the granted ability is decided by the state immediately
 * before the event. By trigger time the creature is gone (no controller to evaluate against), and
 * in a simultaneous event the objects the condition reads may be gone too — so the engine freezes
 * the condition as the creature leaves, taken before the whole batch moves.
 *
 * The anchor is put onto the battlefield *first*, so every one-at-a-time move below takes it
 * before the watcher: a look-back that read the partly-moved state would miss it.
 */
class ConditionalSelfGrantLookBackTest : FunSpec({

    // "As long as you control an artifact, this creature has 'When this creature dies, you gain 3 life.'"
    val watcher = card("Look-Back Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1
        toughness = 1
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantTriggeredAbility(
                    ability = grantedTriggeredAbility {
                        trigger = Triggers.self.dies()
                        effect = Effects.GainLife(3)
                    },
                    filter = GroupFilter.source(),
                ),
                condition = Conditions.YouControl(GameObjectFilter.Artifact),
            )
        }
    }

    val anchor = card("Look-Back Anchor") {
        manaCost = "{0}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
    }

    // One effect, one MoveCollection batch.
    val collectionWipe = card("Collection Wipe") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature) }
    }

    // One effect, iterated one creature at a time.
    val loopWipe = card("Loop Wipe") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.Move(EffectTarget.IterationEntity, Zone.GRAVEYARD, byDestruction = true)
            )
        }
    }

    // Two separate events: the anchor, *then* the watcher.
    val anchorThenWatcher = card("Anchor Then Watcher") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val first = target(TargetFilter(GameObjectFilter.Artifact))
            val second = target(TargetFilter(GameObjectFilter.Creature.nonartifact()))
            effect = Effects.Destroy(first) then Effects.Destroy(second)
        }
    }

    // Kills the watcher alone.
    val killWatcher = card("Kill Watcher") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Creature))
            effect = Effects.Destroy(t)
        }
    }

    fun setup(withAnchor: Boolean): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(watcher, anchor, collectionWipe, loopWipe, anchorThenWatcher, killWatcher))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (withAnchor) driver.putCreatureOnBattlefield(driver.player1, "Look-Back Anchor")
        driver.putCreatureOnBattlefield(driver.player1, "Look-Back Watcher")
        return driver
    }

    fun GameTestDriver.cast(name: String, targets: List<com.wingedsheep.sdk.model.EntityId> = emptyList()) {
        castSpell(player1, putCardInHand(player1, name), targets).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("dies alone while the condition holds — the granted trigger fires") {
        val driver = setup(withAnchor = true)
        driver.cast("Kill Watcher", listOf(driver.findPermanent(driver.player1, "Look-Back Watcher")!!))
        driver.getLifeTotal(driver.player1) shouldBe 23
    }

    test("dies while the condition doesn't hold — no trigger") {
        val driver = setup(withAnchor = false)
        driver.cast("Kill Watcher", listOf(driver.findPermanent(driver.player1, "Look-Back Watcher")!!))
        driver.getLifeTotal(driver.player1) shouldBe 20
    }

    test("destroyed in one MoveCollection batch with the anchor — the look-back still sees it") {
        val driver = setup(withAnchor = true)
        driver.cast("Collection Wipe")
        withClue("the anchor moved first, but the look-back is taken before the batch") {
            driver.getLifeTotal(driver.player1) shouldBe 23
        }
    }

    test("destroyed in one ForEachInGroup loop with the anchor — the look-back still sees it") {
        val driver = setup(withAnchor = true)
        driver.cast("Loop Wipe")
        withClue("the loop is one event; its members' grants are frozen before the first iteration") {
            driver.getLifeTotal(driver.player1) shouldBe 23
        }
    }

    test("the anchor destroyed by an earlier, separate event — no trigger") {
        val driver = setup(withAnchor = true)
        driver.cast(
            "Anchor Then Watcher",
            listOf(
                driver.findPermanent(driver.player1, "Look-Back Anchor")!!,
                driver.findPermanent(driver.player1, "Look-Back Watcher")!!,
            )
        )
        withClue("two sequential destructions are two events; the second looks back to a board with no artifact") {
            driver.getLifeTotal(driver.player1) shouldBe 20
        }
    }
})
