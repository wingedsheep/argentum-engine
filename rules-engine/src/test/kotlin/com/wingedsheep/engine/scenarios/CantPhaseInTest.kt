package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantPhaseIn
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [CantPhaseIn] — "Permanents can't phase in" (The City on the Edge of Forever's Human token).
 *
 * Phased-out permanents phase in during their controller's untap step (CR 702.26a); the static
 * forbids it (CR 101.2: the "can't" wins), so a locked permanent stays phased out and tries again at
 * its controller's next untap step once the lock is gone. The untap step's phase-in is simultaneous,
 * so a lock printer that is itself phasing in doesn't exist yet (CR 702.26b) and stops nothing. A
 * "phases out until [source] leaves" link whose source leaves under the lock just ends. The lock
 * never stops phasing *out*.
 */
class CantPhaseInTest : ScenarioTestBase() {

    private val lock = card("Phase Lock") {
        manaCost = "{2}"; typeLine = "Artifact"
        oracleText = "Permanents can't phase in."
        staticAbility { ability = CantPhaseIn() }
    }

    private val creatureLock = card("Creature Phase Lock") {
        manaCost = "{2}"; typeLine = "Artifact"
        oracleText = "Creatures can't phase in."
        staticAbility { ability = CantPhaseIn(GameObjectFilter.Creature) }
    }

    private val trinket = card("Phase Trinket") {
        manaCost = "{1}"; typeLine = "Artifact"
    }

    private val phaseOut = card("Phase Out Target") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.PhaseOut(t)
        }
    }

    private val phaseOutAll = card("Phase Out Everything") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature),
                Effects.PhaseOut(EffectTarget.IterationEntity)
            )
        }
    }

    private val breakArtifact = card("Break Artifact") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Artifact)
            effect = Effects.Destroy(t)
        }
    }

    private val breakEnchantment = card("Break Enchantment") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Enchantment))
            effect = Effects.Destroy(t)
        }
    }

    private val linkedPrison = card("Linked Prison") {
        manaCost = "{U}"; typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.self.enters()
            val creature = target(TargetFilter.Creature)
            effect = Effects.PhaseOutUntilLeaves(creature, tapOnPhaseIn = true)
        }
        triggeredAbility {
            trigger = Triggers.self.leaves()
            effect = Effects.PhaseInLinkedToSource()
        }
    }

    init {
        listOf(lock, creatureLock, trinket, phaseOut, phaseOutAll, breakArtifact, breakEnchantment, linkedPrison)
            .forEach { cardRegistry.register(it) }

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withLandsOnBattlefield(1, "Island", 4)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(8) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }

        fun TestGame.isPhasedIn(id: EntityId) = id in state.getBattlefield()

        fun TestGame.toUpkeepOf(playerId: EntityId) {
            passUntilPhase(Phase.ENDING, Step.END)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            state.activePlayerId shouldBe playerId
        }

        fun TestGame.toMainOf(playerId: EntityId) {
            toUpkeepOf(playerId)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        }

        test("a phased-out permanent doesn't phase in while the lock is out, and does once it's gone") {
            val game = builder()
                .withCardOnBattlefield(1, lock.name)
                .withCardInHand(1, phaseOut.name)
                .withCardInHand(1, breakArtifact.name)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val lockId = game.findPermanent(lock.name)!!

            withClue("the lock doesn't stop phasing out") {
                game.castSpell(1, phaseOut.name, bears).error shouldBe null
                game.resolveStack()
                game.isPhasedIn(bears) shouldBe false
            }

            withClue("its controller's untap step leaves it phased out") {
                game.toUpkeepOf(game.player2Id)
                game.isPhasedIn(bears) shouldBe false
            }

            game.toMainOf(game.player1Id)
            game.castSpell(1, breakArtifact.name, lockId).error shouldBe null
            game.resolveStack()
            withClue("the lock leaving doesn't phase anything in by itself") {
                game.isPhasedIn(bears) shouldBe false
            }

            withClue("it phases in at its controller's next untap step") {
                game.toUpkeepOf(game.player2Id)
                game.isPhasedIn(bears) shouldBe true
            }
        }

        test("a filtered lock holds only what it matches") {
            val game = builder()
                .withCardOnBattlefield(1, creatureLock.name)
                .withCardOnBattlefield(2, trinket.name)
                .withCardInHand(1, phaseOut.name)
                .withCardInHand(1, phaseOut.name)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val trinketId = game.findPermanent(trinket.name)!!

            game.castSpell(1, phaseOut.name, bears).error shouldBe null
            game.resolveStack()
            game.castSpell(1, phaseOut.name, trinketId).error shouldBe null
            game.resolveStack()

            game.toUpkeepOf(game.player2Id)
            game.isPhasedIn(trinketId) shouldBe true
            game.isPhasedIn(bears) shouldBe false
        }

        test("a lock phasing in alongside the others doesn't exist yet, so everything phases in together") {
            val game = builder()
                .withCardOnBattlefield(2, lock.name)
                .withCardInHand(1, phaseOutAll.name)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val lockId = game.findPermanent(lock.name)!!

            game.castSpell(1, phaseOutAll.name).error shouldBe null
            game.resolveStack()
            game.isPhasedIn(bears) shouldBe false
            game.isPhasedIn(lockId) shouldBe false

            game.toUpkeepOf(game.player2Id)
            game.isPhasedIn(lockId) shouldBe true
            game.isPhasedIn(bears) shouldBe true
        }

        test("a linked phase-in under the lock just ends; the permanent phases in later, untapped") {
            val game = builder()
                .withCardOnBattlefield(1, lock.name)
                .withCardInHand(1, linkedPrison.name)
                .withCardInHand(1, breakEnchantment.name)
                .withCardInHand(1, breakArtifact.name)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val lockId = game.findPermanent(lock.name)!!

            game.castSpell(1, linkedPrison.name).error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.isPhasedIn(bears) shouldBe false
            val prison = game.findPermanent(linkedPrison.name)!!

            game.castSpell(1, breakEnchantment.name, prison).error shouldBe null
            game.resolveStack()
            withClue("the source left, but the lock forbids the phase-in; the link is spent") {
                game.isPhasedIn(bears) shouldBe false
                val phased = game.state.getEntity(bears)!!.get<PhasedOutComponent>()!!
                phased.phaseInOnSourceLeaves shouldBe null
                phased.tapOnPhaseIn shouldBe false
            }

            game.toUpkeepOf(game.player2Id)
            game.isPhasedIn(bears) shouldBe false

            game.toMainOf(game.player1Id)
            game.castSpell(1, breakArtifact.name, lockId).error shouldBe null
            game.resolveStack()

            withClue("with the lock gone it phases in at its controller's untap step, like any phased-out permanent") {
                game.toUpkeepOf(game.player2Id)
                game.isPhasedIn(bears) shouldBe true
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
            }
        }
    }
}
