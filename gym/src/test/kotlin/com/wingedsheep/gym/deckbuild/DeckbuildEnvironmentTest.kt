package com.wingedsheep.gym.deckbuild

import com.wingedsheep.gym.contract.DeckbuildObservation
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the deckbuild env: the add/remove/finalize loop, pool-copy limits,
 * unlimited basics, the finalize gate, and the intrinsic deck score.
 */
class DeckbuildEnvironmentTest : FunSpec({

    fun bear(name: String) =
        CardDefinition.creature(name, ManaCost.parse("{1}{G}"), setOf(Subtype.of("Bear")), 2, 2)

    val forest = CardDefinition.basicLand("Forest", Subtype.of("Forest"))

    /** A pool of 3 distinct creatures, 2 copies each. */
    fun pool() = listOf("Grizzly", "Runeclaw", "Balduvian").flatMap { listOf(bear(it), bear(it)) }

    fun obs(env: DeckbuildEnvironment) = env.observe().observation as DeckbuildObservation

    fun idOf(env: DeckbuildEnvironment, kind: String, nameContains: String): Int =
        obs(env).legalActions.first { it.kind == kind && it.description.contains(nameContains) }.actionId

    test("opening observation lists the pooled cards with their copy counts") {
        val env = DeckbuildEnvironment(pool(), listOf(forest), targetSize = 5)
        val o = obs(env)
        o.terminated.shouldBeFalse()
        o.selectedCount shouldBe 0
        o.pool.map { it.name }.toSet() shouldBe setOf("Grizzly", "Runeclaw", "Balduvian")
        o.pool.all { it.remaining == 2 }.shouldBeTrue()
        // basics are offered separately and addable
        o.basics.map { it.name } shouldContain "Forest"
    }

    test("adding a card decrements its remaining count and grows the deck") {
        val env = DeckbuildEnvironment(pool(), listOf(forest), targetSize = 5)
        env.step(idOf(env, "ADD_CARD", "Grizzly"))
        val o = obs(env)
        o.selected["Grizzly"] shouldBe 1
        o.selectedCount shouldBe 1
        o.pool.first { it.name == "Grizzly" }.remaining shouldBe 1
    }

    test("cannot add more copies than the pool holds") {
        val env = DeckbuildEnvironment(pool(), listOf(forest), targetSize = 5)
        env.step(idOf(env, "ADD_CARD", "Grizzly"))
        env.step(idOf(env, "ADD_CARD", "Grizzly"))
        val o = obs(env)
        o.selected["Grizzly"] shouldBe 2
        // No ADD_CARD for Grizzly remains once both copies are taken.
        o.legalActions.any { it.kind == "ADD_CARD" && it.description.contains("Grizzly") }.shouldBeFalse()
    }

    test("basics can be added without limit") {
        val env = DeckbuildEnvironment(pool(), listOf(forest), targetSize = 5)
        repeat(10) { env.step(idOf(env, "ADD_BASIC", "Forest")) }
        obs(env).selected["Forest"] shouldBe 10
    }

    test("remove undoes an add") {
        val env = DeckbuildEnvironment(pool(), listOf(forest), targetSize = 5)
        env.step(idOf(env, "ADD_CARD", "Grizzly"))
        env.step(idOf(env, "REMOVE_CARD", "Grizzly"))
        val o = obs(env)
        o.selected.containsKey("Grizzly").shouldBeFalse()
        o.pool.first { it.name == "Grizzly" }.remaining shouldBe 2
    }

    test("FINALIZE is gated on reaching the target size, then terminates with a score") {
        val env = DeckbuildEnvironment(pool(), listOf(forest), targetSize = 5)
        // Below target: no FINALIZE offered.
        obs(env).legalActions.any { it.kind == "FINALIZE" }.shouldBeFalse()

        // 3 spells + 2 basics = 5.
        env.step(idOf(env, "ADD_CARD", "Grizzly"))
        env.step(idOf(env, "ADD_CARD", "Runeclaw"))
        env.step(idOf(env, "ADD_CARD", "Balduvian"))
        env.step(idOf(env, "ADD_BASIC", "Forest"))
        env.step(idOf(env, "ADD_BASIC", "Forest"))
        obs(env).selectedCount shouldBe 5

        env.step(idOf(env, "FINALIZE", "Finalize"))
        val o = obs(env)
        o.terminated.shouldBeTrue()
        o.agentToAct.shouldBeNull()
        o.legalActions.isEmpty().shouldBeTrue()
        o.deckScore.shouldNotBeNull()
        // Three rated creatures contribute a positive score; basics contribute nothing.
        o.deckScore!! shouldBeGreaterThan 0.0
        env.isTerminal.shouldBeTrue()
        env.finalDeck shouldBe mapOf("Grizzly" to 1, "Runeclaw" to 1, "Balduvian" to 1, "Forest" to 2)
    }

    test("fork preserves the in-progress selection but diverges after") {
        val env = DeckbuildEnvironment(pool(), listOf(forest), targetSize = 5)
        env.step(idOf(env, "ADD_CARD", "Grizzly"))
        val forked = env.fork() as DeckbuildEnvironment
        (forked.observe().observation as DeckbuildObservation).selected["Grizzly"] shouldBe 1

        forked.step(idOf(forked, "ADD_CARD", "Runeclaw"))
        // The source is unaffected by the fork's extra pick.
        obs(env).selected.containsKey("Runeclaw").shouldBeFalse()
        (forked.observe().observation as DeckbuildObservation).selected["Runeclaw"] shouldBe 1
    }
})
