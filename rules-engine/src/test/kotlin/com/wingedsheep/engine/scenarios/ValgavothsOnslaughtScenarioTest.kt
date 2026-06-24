package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.ManifestedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Valgavoth's Onslaught ({X}{X}{G}) — Duskmourn: House of Horror.
 *
 * "Manifest dread X times, then put X +1/+1 counters on each of those creatures."
 *
 * Exercises the manifest-dread accumulation capability: `RepeatDynamicTimes(XValue,
 * manifestDread(accumulateInto = "valgavothManifested"))` grows one collection of every manifested
 * permanent across all X iterations (via MoveCollection's `accumulateMoved` flag), and
 * `AddCountersToCollection` then puts X +1/+1 counters on each of them. The chosen value of X (not
 * the doubled {X}{X} mana) drives both the repeat count and the counter count.
 */
class ValgavothsOnslaughtScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    /** Resolve one manifest-dread pick (manifest the named card; bin the other). */
    fun GameTestDriver.resolveManifestPick(you: EntityId, manifest: EntityId) {
        val pick = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitDecision(you, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(manifest)))
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    test("X=2 manifests dread twice and puts 2 +1/+1 counters on each manifested creature") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Top of library (top-most last): for the first manifest dread, look at [c1, land1];
        // for the second, look at [c2, land2]. We manifest the creatures both times.
        val land2 = d.putCardOnTopOfLibrary(you, "Forest")
        val c2 = d.putCardOnTopOfLibrary(you, "Centaur Courser")
        val land1 = d.putCardOnTopOfLibrary(you, "Forest")
        val c1 = d.putCardOnTopOfLibrary(you, "Centaur Courser") // current top card

        // Cast with X=2 → {X}{X}{G} = {4}{G} = 5 mana (4 generic + 1 green).
        val spell = d.putCardInHand(you, "Valgavoth's Onslaught")
        d.giveMana(you, Color.GREEN, 5)
        d.castXSpell(you, spell, xValue = 2)
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // First manifest dread looks at the top two (c1 on top, land1 beneath); manifest c1.
        d.resolveManifestPick(you, c1)
        // Second manifest dread looks at the next two (c2, land2); manifest c2.
        d.resolveManifestPick(you, c2)

        // Both creatures manifested as face-down 2/2s, each then gets 2 +1/+1 counters → 4/4.
        d.getPermanents(you).shouldContainAll(listOf(c1, c2))
        for (creature in listOf(c1, c2)) {
            val entity = d.state.getEntity(creature)
            entity?.get<FaceDownComponent>() shouldBe FaceDownComponent
            entity?.get<ManifestedComponent>() shouldBe ManifestedComponent
            // base 2/2 + two +1/+1 counters = 4/4 (the counters landed on *each* manifested creature).
            d.state.projectedState.getPower(creature) shouldBe 4
            d.state.projectedState.getToughness(creature) shouldBe 4
        }
    }

    test("X=0 manifests nothing and places no counters") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val permanentsBefore = d.getPermanents(you).toSet()

        // X=0 → {G} = 1 green mana; RepeatDynamicTimes no-ops and the accumulator is empty.
        val spell = d.putCardInHand(you, "Valgavoth's Onslaught")
        d.giveMana(you, Color.GREEN, 1)
        d.castXSpell(you, spell, xValue = 0)
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.isPaused shouldBe false
        // No new manifested permanents entered the battlefield.
        d.getPermanents(you).toSet() shouldBe permanentsBefore
    }
})
