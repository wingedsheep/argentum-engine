package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Name-choice predicates against frozen last-known information
 * ([PredicateEvaluator.matchesSnapshot]): "a source with the chosen name" asked of an object its
 * own cost sacrificed (Kai Winn, Faithless Leader). [CardPredicate.NameEqualsChosenComponent] reads
 * the choosing permanent (the context's source) and compares it with the snapshot's frozen name;
 * [CardPredicate.NameEqualsChosen] does the same with a pipeline variable. A face-down object has
 * no name (CR 708.2a); a snapshot that froze no name stays unknown, so a negation doesn't match.
 */
class SnapshotChosenNameMatchTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)

    fun setup(chosen: String?): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val chooser = driver.putPermanentOnBattlefield(driver.player1, "Grizzly Bears")
        if (chosen != null) {
            driver.replaceState(driver.state.updateEntity(chooser) {
                it.with(CastChoicesComponent().withChoice(ChoiceSlot.CARD_NAME, ChoiceValue.TextChoice(chosen)))
            })
        }
        return driver to chooser
    }

    fun snapshot(name: String?, faceDown: Boolean = false) = EntitySnapshot(
        entityId = EntityId.generate(),
        typeLine = TypeLine.parse("Artifact"),
        name = name,
        wasFaceDown = faceDown,
    )

    fun GameTestDriver.snapshotMatches(
        snap: EntitySnapshot,
        filter: GameObjectFilter,
        context: PredicateContext,
    ) = evaluator.matchesSnapshot(state, snap, filter, context, onBattlefield = true)

    val named = GameObjectFilter.Any.namedFromChosenComponent()

    test("a departed source matches the name its chooser picked, case-insensitively") {
        val (driver, chooser) = setup("Mind Stone")
        val ctx = PredicateContext(controllerId = driver.player1, sourceId = chooser)
        driver.snapshotMatches(snapshot("Mind Stone"), named, ctx) shouldBe true
        driver.snapshotMatches(snapshot("mind stone"), named, ctx) shouldBe true
    }

    test("a departed source with another name doesn't match, and does match the negation") {
        val (driver, chooser) = setup("Mind Stone")
        val ctx = PredicateContext(controllerId = driver.player1, sourceId = chooser)
        driver.snapshotMatches(snapshot("Ichor Wellspring"), named, ctx) shouldBe false
        val notNamed = GameObjectFilter.Any.withCardPredicate(
            CardPredicate.Not(CardPredicate.NameEqualsChosenComponent())
        )
        driver.snapshotMatches(snapshot("Ichor Wellspring"), notNamed, ctx) shouldBe true
    }

    test("a face-down departed object has no name, so it never matches a chosen name") {
        val (driver, chooser) = setup("Mind Stone")
        val ctx = PredicateContext(controllerId = driver.player1, sourceId = chooser)
        driver.snapshotMatches(snapshot("Mind Stone", faceDown = true), named, ctx) shouldBe false
    }

    test("no choice on the source, or no source, matches nothing") {
        val (driver, chooser) = setup(null)
        driver.snapshotMatches(
            snapshot("Mind Stone"), named, PredicateContext(controllerId = driver.player1, sourceId = chooser)
        ) shouldBe false
        driver.snapshotMatches(
            snapshot("Mind Stone"), named, PredicateContext(controllerId = driver.player1)
        ) shouldBe false
    }

    test("a snapshot that froze no name stays unknown — neither it nor its negation matches") {
        val (driver, chooser) = setup("Mind Stone")
        val ctx = PredicateContext(controllerId = driver.player1, sourceId = chooser)
        driver.snapshotMatches(snapshot(null), named, ctx) shouldBe false
        val notNamed = GameObjectFilter.Any.withCardPredicate(
            CardPredicate.Not(CardPredicate.NameEqualsChosenComponent())
        )
        driver.snapshotMatches(snapshot(null), notNamed, ctx) shouldBe false
    }

    test("a pipeline-variable name is read the same way") {
        val (driver, _) = setup(null)
        val filter = GameObjectFilter.Any.withCardPredicate(CardPredicate.NameEqualsChosen("picked"))
        val ctx = PredicateContext(controllerId = driver.player1, chosenValues = mapOf("picked" to "Mind Stone"))
        driver.snapshotMatches(snapshot("Mind Stone"), filter, ctx) shouldBe true
        driver.snapshotMatches(snapshot("Sol Ring"), filter, ctx) shouldBe false
    }
})
