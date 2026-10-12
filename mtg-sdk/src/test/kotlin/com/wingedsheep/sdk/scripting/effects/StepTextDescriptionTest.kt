package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.HandPatterns
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldNotStartWith

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

    describe("a discard from your own hand reads as discard") {

        it("Yuyan Archers: you may discard a card. If you do, draw a card") {
            Effects.May(
                effect = Effects.IfYouDo(action = HandPatterns.discardCards(1), then = Effects.DrawCards(1))
            ).description shouldBe "You may discard a card. If you do, draw a card"
        }

        it("several cards take a number word") {
            HandPatterns.discardCards(2).description shouldBe "Discard two cards"
        }

        it("a filtered discard names the card type") {
            HandPatterns.discardCards(1, filter = GameObjectFilter.Creature).description shouldBe
                "Discard a creature card"
            HandPatterns.discardCards(1, filter = GameObjectFilter.Artifact).description shouldBe
                "Discard an artifact card"
        }

        it("X, any number, and at random") {
            HandPatterns.discardCards(DynamicAmount.XValue).description shouldBe "Discard X cards"
            HandPatterns.discardAnyNumber().description shouldBe "Discard any number of cards"
            HandPatterns.discardRandom(1).description shouldBe "Discard a card at random"
        }

        it("up to N") {
            CompositeEffect(
                listOf(
                    GatherCardsEffect(CardSource.FromZone(Zone.HAND, Player.You), storeAs = "h"),
                    SelectFromCollectionEffect("h", SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)), storeSelected = "d"),
                    MoveCollectionEffect("d", CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Discard),
                )
            ).description shouldBe "Discard up to one card"
        }

        it("another player's discard keeps its per-step text") {
            HandPatterns.discardCards(1, target = EffectTarget.ContextTarget(0)).description shouldNotStartWith "Discard"
        }

        it("a plain move from hand to graveyard is not a discard") {
            CompositeEffect(
                listOf(
                    GatherCardsEffect(CardSource.FromZone(Zone.HAND, Player.You), storeAs = "h"),
                    SelectFromCollectionEffect("h", SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)), storeSelected = "d"),
                    MoveCollectionEffect("d", CardDestination.ToZone(Zone.GRAVEYARD)),
                )
            ).description shouldNotStartWith "Discard"
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

    describe("an empty else branch adds no dangling \"Otherwise,\"") {

        it("a collection test whose else is Effects.Nothing (Diregraf Scavenger)") {
            ConditionalOnCollectionEffect(
                collection = "exiled",
                ifNotEmpty = Effects.GainLife(2),
                ifEmpty = Effects.Nothing,
                filter = GameObjectFilter.Creature
            ).description shouldBe "If those cards include a creature card, you gain 2 life"
        }

        it("a collection test still names a real else branch") {
            ConditionalOnCollectionEffect(
                collection = "exiled",
                ifNotEmpty = Effects.GainLife(2),
                ifEmpty = Effects.DrawCards(1)
            ).description shouldNotContain "Otherwise, ."
        }

        it("a gate whose otherwise is Effects.Nothing") {
            Effects.If(
                condition = com.wingedsheep.sdk.dsl.Conditions.OpponentControlsMoreLands,
                then = Effects.GainLife(2),
                otherwise = Effects.Nothing
            ).description shouldNotContain "Otherwise"
        }
    }

    describe("a library end with a counted phrase reads as Oracle does") {

        it("a fixed count stays between top and cards") {
            CardSource.TopOfLibrary(3, Player.You).description shouldBe "the top 3 cards of your library"
            CardSource.TopOfLibrary(DynamicAmount.XValue, Player.You).description shouldBe
                "the top X cards of your library"
        }

        it("a phrase count moves after the library (Screaming Swarm)") {
            LibraryPatterns.mill(
                com.wingedsheep.sdk.dsl.DynamicAmounts.attackingCreaturesYouControl(),
                EffectTarget.ContextTarget(0)
            ).description shouldNotContain "top the number"
            CardSource.BottomOfLibrary(
                com.wingedsheep.sdk.dsl.DynamicAmounts.attackingCreaturesYouControl(), Player.You
            ).description shouldBe "a number of cards from the bottom of your library equal to " +
                com.wingedsheep.sdk.dsl.DynamicAmounts.attackingCreaturesYouControl().description
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
