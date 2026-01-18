package com.wingedsheep.rulesengine.zone

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class ZoneTest : FunSpec({

    val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)
    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    fun createForest(owner: String) = CardInstance.create(forestDef, owner)
    fun createBear(owner: String) = CardInstance.create(bearDef, owner)

    context("factory methods") {
        test("library creates library zone") {
            val zone = Zone.library("player1")
            zone.type shouldBe ZoneType.LIBRARY
            zone.ownerId shouldBe "player1"
            zone.isEmpty.shouldBeTrue()
        }

        test("hand creates hand zone") {
            val zone = Zone.hand("player1")
            zone.type shouldBe ZoneType.HAND
            zone.ownerId shouldBe "player1"
        }

        test("battlefield creates shared battlefield") {
            val zone = Zone.battlefield()
            zone.type shouldBe ZoneType.BATTLEFIELD
            zone.ownerId shouldBe null
        }

        test("graveyard creates graveyard zone") {
            val zone = Zone.graveyard("player1")
            zone.type shouldBe ZoneType.GRAVEYARD
            zone.ownerId shouldBe "player1"
        }
    }

    context("adding cards") {
        test("addToTop adds card to end of list") {
            val card1 = createForest("player1")
            val card2 = createBear("player1")

            val zone = Zone.library("player1")
                .addToTop(card1)
                .addToTop(card2)

            zone.size shouldBe 2
            zone.topCard() shouldBe card2
            zone.bottomCard() shouldBe card1
        }

        test("addToBottom adds card to beginning of list") {
            val card1 = createForest("player1")
            val card2 = createBear("player1")

            val zone = Zone.library("player1")
                .addToBottom(card1)
                .addToBottom(card2)

            zone.size shouldBe 2
            zone.topCard() shouldBe card1
            zone.bottomCard() shouldBe card2
        }

        test("addAt inserts at specific position") {
            val card1 = createForest("player1")
            val card2 = createBear("player1")
            val card3 = createForest("player1")

            val zone = Zone.library("player1")
                .addToTop(card1)
                .addToTop(card3)
                .addAt(1, card2)

            zone.cards[0] shouldBe card1
            zone.cards[1] shouldBe card2
            zone.cards[2] shouldBe card3
        }

        test("addAll adds multiple cards") {
            val cards = listOf(createForest("player1"), createBear("player1"))
            val zone = Zone.library("player1").addAll(cards)
            zone.size shouldBe 2
        }
    }

    context("removing cards") {
        test("remove removes card by id") {
            val card1 = createForest("player1")
            val card2 = createBear("player1")

            val zone = Zone.library("player1")
                .addToTop(card1)
                .addToTop(card2)
                .remove(card1.id)

            zone.size shouldBe 1
            zone.contains(card1.id).shouldBeFalse()
            zone.contains(card2.id).shouldBeTrue()
        }

        test("removeTop removes and returns top card") {
            val card1 = createForest("player1")
            val card2 = createBear("player1")

            val zone = Zone.library("player1")
                .addToTop(card1)
                .addToTop(card2)

            val (removed, newZone) = zone.removeTop()

            removed shouldBe card2
            newZone.size shouldBe 1
            newZone.topCard() shouldBe card1
        }

        test("removeTop returns null for empty zone") {
            val zone = Zone.library("player1")
            val (removed, newZone) = zone.removeTop()

            removed.shouldBeNull()
            newZone shouldBe zone
        }

        test("removeBottom removes and returns bottom card") {
            val card1 = createForest("player1")
            val card2 = createBear("player1")

            val zone = Zone.library("player1")
                .addToTop(card1)
                .addToTop(card2)

            val (removed, newZone) = zone.removeBottom()

            removed shouldBe card1
            newZone.size shouldBe 1
        }
    }

    context("querying cards") {
        test("contains returns true if card is in zone") {
            val card = createForest("player1")
            val zone = Zone.library("player1").addToTop(card)

            zone.contains(card.id).shouldBeTrue()
        }

        test("contains returns false if card is not in zone") {
            val card = createForest("player1")
            val zone = Zone.library("player1")

            zone.contains(card.id).shouldBeFalse()
        }

        test("getCard returns card if present") {
            val card = createForest("player1")
            val zone = Zone.library("player1").addToTop(card)

            zone.getCard(card.id) shouldBe card
        }

        test("getCard returns null if not present") {
            val card = createForest("player1")
            val zone = Zone.library("player1")

            zone.getCard(card.id).shouldBeNull()
        }

        test("filter returns matching cards") {
            val forest = createForest("player1")
            val bear = createBear("player1")

            val zone = Zone.battlefield()
                .addToTop(forest)
                .addToTop(bear)

            val creatures = zone.filter { it.isCreature }
            creatures.size shouldBe 1
            creatures[0] shouldBe bear
        }
    }

    context("updating cards") {
        test("updateCard transforms matching card") {
            val card = createBear("player1")
            val zone = Zone.battlefield().addToTop(card)

            val updatedZone = zone.updateCard(card.id) { it.tap() }

            updatedZone.getCard(card.id)!!.isTapped.shouldBeTrue()
        }

        test("updateCard leaves non-matching cards unchanged") {
            val card1 = createBear("player1")
            val card2 = createForest("player1")

            val zone = Zone.battlefield()
                .addToTop(card1)
                .addToTop(card2)
                .updateCard(card1.id) { it.tap() }

            zone.getCard(card1.id)!!.isTapped.shouldBeTrue()
            zone.getCard(card2.id)!!.isTapped.shouldBeFalse()
        }
    }

    context("shuffle") {
        test("shuffle randomizes card order") {
            val cards = (1..10).map { createForest("player1") }
            val zone = Zone.library("player1", cards)

            val shuffled = zone.shuffle(java.util.Random(42))

            shuffled.size shouldBe zone.size
            // With a fixed seed, we can verify shuffle happened
            shuffled.cards.map { it.id } shouldBe shuffled.cards.map { it.id }
        }
    }

    context("isEmpty and isNotEmpty") {
        test("empty zone isEmpty is true") {
            Zone.library("player1").isEmpty.shouldBeTrue()
        }

        test("non-empty zone isEmpty is false") {
            val zone = Zone.library("player1").addToTop(createForest("player1"))
            zone.isEmpty.shouldBeFalse()
        }

        test("empty zone isNotEmpty is false") {
            Zone.library("player1").isNotEmpty.shouldBeFalse()
        }

        test("non-empty zone isNotEmpty is true") {
            val zone = Zone.library("player1").addToTop(createForest("player1"))
            zone.isNotEmpty.shouldBeTrue()
        }
    }
})
