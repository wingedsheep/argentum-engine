package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.LibrarySearchedEvent
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ShieldWallSentinel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ShieldWallSentinelScenarioTest : FunSpec({
    val nondefender = card("Sentinel Test Creature") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }
    // A keyword alone must not satisfy the Oracle's creature-card restriction.
    val noncreature = card("Sentinel Test Artifact") {
        manaCost = "{1}"
        typeLine = "Artifact"
        keywords(Keyword.DEFENDER)
    }
    fun driver() = GameTestDriver().apply {
        registerCards(listOf(ShieldWallSentinel, DominariaUnitedForest274, nondefender, noncreature))
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun castSentinel(d: GameTestDriver): com.wingedsheep.sdk.model.EntityId {
        val sentinel = d.putCardInHand(d.player1, "Shield-Wall Sentinel")
        d.giveMana(d.player1, Color.GREEN, 4)
        d.castSpell(d.player1, sentinel).outcome shouldBe Outcome.Done
        d.bothPass()
        d.stackSize shouldBe 1
        return sentinel
    }
    fun resolveToMay(d: GameTestDriver) {
        d.bothPass()
        (d.pendingDecision is YesNoDecision) shouldBe true
    }

    for (sourceLeaves in listOf(false, true)) {
        test("search offers only defender creatures and reveals the chosen card; source leaves = $sourceLeaves") {
            val d = driver()
            val defender = d.putCardOnTopOfLibrary(d.player1, "Shield-Wall Sentinel")
            d.putCardOnTopOfLibrary(d.player1, nondefender.name)
            d.putCardOnTopOfLibrary(d.player1, noncreature.name)
            val sentinel = castSentinel(d)
            if (sourceLeaves) d.moveToGraveyard(sentinel)
            val eventCount = d.events.size
            resolveToMay(d)
            d.submitYesNo(d.player1, true)
            val search = d.pendingDecision as SelectCardsDecision
            search.options shouldBe listOf(defender)
            d.submitCardSelection(d.player1, listOf(defender))
            d.pendingDecision shouldBe null
            d.stackSize shouldBe 0
            (defender in d.getHand(d.player1)) shouldBe true
            d.events.drop(eventCount).filterIsInstance<CardsRevealedEvent>().any { defender in it.cardIds } shouldBe true
            d.events.drop(eventCount).filterIsInstance<LibrarySearchedEvent>().size shouldBe 1
            d.events.drop(eventCount).filterIsInstance<LibraryShuffledEvent>().size shouldBe 1
            if (sourceLeaves) {
                (sentinel in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe true
            }
        }
    }

    test("declining the optional search neither looks nor shuffles") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Shield-Wall Sentinel")
        castSentinel(d)
        val libraryBefore = d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY))
        val eventCount = d.events.size
        resolveToMay(d)
        d.submitYesNo(d.player1, false)
        d.pendingDecision shouldBe null
        d.stackSize shouldBe 0
        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)) shouldBe libraryBefore
        d.events.drop(eventCount).filterIsInstance<LibrarySearchedEvent>().size shouldBe 0
        d.events.drop(eventCount).filterIsInstance<LibraryShuffledEvent>().size shouldBe 0
    }

    test("accepting the search may fail to find but still shuffles") {
        val d = driver()
        val defender = d.putCardOnTopOfLibrary(d.player1, "Shield-Wall Sentinel")
        castSentinel(d)
        val eventCount = d.events.size
        resolveToMay(d)
        d.submitYesNo(d.player1, true)
        d.submitCardSelection(d.player1, emptyList())
        d.pendingDecision shouldBe null
        d.stackSize shouldBe 0
        (defender in d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY))) shouldBe true
        d.events.drop(eventCount).filterIsInstance<LibrarySearchedEvent>().size shouldBe 1
        d.events.drop(eventCount).filterIsInstance<LibraryShuffledEvent>().size shouldBe 1
        d.events.drop(eventCount).filterIsInstance<CardsRevealedEvent>().size shouldBe 0
    }
})
