package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Whenever you copy a spell" (`Triggers.you.copies`) and magecraft's "cast or copy"
 * (`Triggers.you.castsOrCopies`).
 *
 * CR 707.10: a copy of a spell isn't cast, it is controlled by the player under whose control it
 * was put on the stack, and it is itself a spell. So a copy fires the copy half but never a cast
 * trigger, "you" is the copy's controller, and the spell filter reads the copy's characteristics.
 */
class SpellCopiedTriggerTest : FunSpec({

    // Gains 1 life per copied instant or sorcery.
    val copyWatcher = card("Copy Watcher") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.copies(GameObjectFilter.InstantOrSorcery)
            effect = Effects.GainLife(1)
        }
    }

    // Gains 10 life per cast-or-copied instant or sorcery.
    val magecraftWatcher = card("Magecraft Watcher") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
            effect = Effects.GainLife(10)
        }
    }

    val quiet = card("Quiet Instant") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    val copier = card("Spell Copier") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val original = target(TargetFilter.SpellOnStack)
            effect = Effects.CopyTargetSpell(original)
        }
    }

    val freeBear = card("Free Bear") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copyWatcher, magecraftWatcher, quiet, copier, freeBear))
        it.initMirrorMatch(deck = Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveAll() {
        repeat(30) {
            val decision = state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                submitDecision(decision.playerId, TargetsResponse(decision.id,
                    decision.legalTargets.mapValues { (_, targets) -> targets.take(1) })).error shouldBe null
            } else if (decision != null) autoResolveDecision()
            else if (stackSize > 0) bothPass()
            else return
        }
        error("stack did not finish resolving")
    }

    fun GameTestDriver.cast(player: EntityId, name: String): EntityId {
        val id = putCardInHand(player, name)
        castSpell(player, id).outcome shouldBe Outcome.Done
        return id
    }

    fun GameTestDriver.copy(player: EntityId, spell: EntityId) {
        castSpellWithTargets(player, putCardInHand(player, copier.name), listOf(ChosenTarget.Spell(spell)))
            .outcome shouldBe Outcome.Done
    }

    test("casting an instant fires the cast-or-copy trigger but not the copy trigger") {
        val game = driver()
        val me = game.activePlayer!!
        game.putPermanentOnBattlefield(me, copyWatcher.name)
        game.putPermanentOnBattlefield(me, magecraftWatcher.name)

        game.cast(me, quiet.name)
        game.resolveAll()

        game.getLifeTotal(me) shouldBe 30
    }

    test("copying an instant fires both triggers once for the copy (CR 707.10: the copy isn't cast)") {
        val game = driver()
        val me = game.activePlayer!!
        game.putPermanentOnBattlefield(me, copyWatcher.name)
        game.putPermanentOnBattlefield(me, magecraftWatcher.name)

        val quietId = game.cast(me, quiet.name) // magecraft +10
        game.copy(me, quietId) // copier is cast: magecraft +10; its copy: copy +1, magecraft +10
        game.resolveAll()

        game.getLifeTotal(me) shouldBe 51
    }

    test("a copy an opponent puts on the stack is theirs: your copy trigger doesn't fire, theirs does") {
        val game = driver()
        val me = game.activePlayer!!
        val opponent = game.getOpponent(me)
        game.putPermanentOnBattlefield(me, copyWatcher.name)
        game.putPermanentOnBattlefield(opponent, copyWatcher.name)

        val quietId = game.cast(me, quiet.name)
        game.passPriority(me)
        game.copy(opponent, quietId)
        game.resolveAll()

        game.getLifeTotal(me) shouldBe 20
        game.getLifeTotal(opponent) shouldBe 21
    }

    test("the spell filter reads the copy: copying a creature spell doesn't fire an instant-or-sorcery trigger") {
        val game = driver()
        val me = game.activePlayer!!
        game.putPermanentOnBattlefield(me, copyWatcher.name)

        val bear = game.cast(me, freeBear.name)
        game.copy(me, bear)
        game.resolveAll()

        game.getLifeTotal(me) shouldBe 20
    }
})
