package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Player-facing text for pipelines whose steps only read as English together: a library search,
 * a battlefield gather and its consumer, and the scry / surveil picks' prompts.
 */
class StepTextDescriptionTest : DescribeSpec({

    val islandSwampMountain = GameObjectFilter.BasicLand.withAnyOfSubtypes(
        listOf(Subtype.ISLAND, Subtype.SWAMP, Subtype.MOUNTAIN)
    )

    describe("a library search reads as one Oracle sentence") {

        it("a Landscape: search, put onto the battlefield tapped, then shuffle") {
            LibraryPatterns.searchLibrary(
                filter = islandSwampMountain,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
            ).description shouldBe
                "Search your library for a basic Island, Swamp, or Mountain card, " +
                "put it onto the battlefield tapped, then shuffle"
        }

        it("a revealed tutor to hand") {
            LibraryPatterns.searchLibrary(filter = GameObjectFilter.Creature, reveal = true).description shouldBe
                "Search your library for a creature card, reveal it, put it into your hand, then shuffle"
        }

        it("several cards take the plural pronoun") {
            LibraryPatterns.searchLibrary(filter = GameObjectFilter.BasicLand, count = 2).description shouldBe
                "Search your library for up to two basic land cards, put them into your hand, then shuffle"
        }

        it("a search to the top shuffles first") {
            LibraryPatterns.searchLibrary(destination = SearchDestination.TOP_OF_LIBRARY).description shouldBe
                "Search your library for a card, then shuffle and put that card on top of your library"
        }

        it("matches the pick's prompt wording") {
            val search = LibraryPatterns.searchLibrary(
                filter = islandSwampMountain,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
            )
            search.effects.filterIsInstance<SelectFromCollectionEffect>().single().prompt shouldBe
                "Search your library for a basic Island, Swamp, or Mountain card to put onto the battlefield tapped"
        }

        it("a search gather on its own says search, not look at") {
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                storeAs = "x",
                search = true,
            ).description shouldBe "Search your library for basic land cards"
        }
    }

    describe("a battlefield gather is its consumer's object") {

        val newcomers = GameObjectFilter.Creature
            .withAnyOfSubtypes(listOf(Subtype("Frog"), Subtype("Rabbit"), Subtype("Raccoon"), Subtype("Squirrel")))
            .enteredThisTurn()

        it("Oakhollow Village's counters land on each matching permanent you control") {
            CompositeEffect(
                listOf(
                    GatherCardsEffect(CardSource.FromZone(Zone.BATTLEFIELD, Player.You, newcomers), storeAs = "c"),
                    AddCountersToCollectionEffect("c", CounterType.PLUS_ONE_PLUS_ONE, 1),
                )
            ).description shouldBe
                "Put a +1/+1 counter on each Frog, Rabbit, Raccoon, or Squirrel creature you control " +
                "that entered the battlefield this turn"
        }

        it("the battlefield is described by control, not as a player's zone") {
            CardSource.FromZone(Zone.BATTLEFIELD, Player.You, GameObjectFilter.Creature).description shouldBe
                "creatures you control"
        }
    }

    describe("filter wording puts subtypes before the type and clauses after it") {

        it("an any-of-subtypes list leads the type word") {
            GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype("Frog"), Subtype("Rabbit")))
                .description shouldBe "Frog or Rabbit creature"
        }

        it("a one-word state still leads, a clause trails with a relative that") {
            GameObjectFilter.Creature.tapped().description shouldBe "tapped creature"
            GameObjectFilter.Creature.enteredThisTurn().description shouldBe
                "creature that entered the battlefield this turn"
        }
    }

    describe("scry and surveil picks name the action and what a selected card means") {

        fun pick(effect: CompositeEffect) = effect.effects.filterIsInstance<SelectFromCollectionEffect>().single()

        it("scry") {
            pick(LibraryPatterns.scryPipeline(2)).prompt shouldBe
                "Scry 2: choose cards to put on the bottom of your library"
        }

        it("surveil") {
            pick(LibraryPatterns.surveilPipeline(1)).prompt shouldBe
                "Surveil 1: choose cards to put into your graveyard"
        }
    }
})
