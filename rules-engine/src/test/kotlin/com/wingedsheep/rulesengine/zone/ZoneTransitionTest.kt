package com.wingedsheep.rulesengine.zone

import com.wingedsheep.rulesengine.core.CardId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse

class ZoneTransitionTest : FunSpec({

    val cardId = CardId.generate()
    val cardName = "Test Card"

    context("entersBattlefield") {
        test("true when moving to battlefield from hand") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.HAND,
                to = ZoneType.BATTLEFIELD,
                fromOwnerId = "player1",
                toOwnerId = null
            )
            transition.entersBattlefield.shouldBeTrue()
        }

        test("true when moving to battlefield from stack") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.STACK,
                to = ZoneType.BATTLEFIELD,
                fromOwnerId = null,
                toOwnerId = null
            )
            transition.entersBattlefield.shouldBeTrue()
        }

        test("false when already on battlefield") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.BATTLEFIELD,
                to = ZoneType.BATTLEFIELD,
                fromOwnerId = null,
                toOwnerId = null
            )
            transition.entersBattlefield.shouldBeFalse()
        }
    }

    context("leavesBattlefield") {
        test("true when moving from battlefield to graveyard") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.BATTLEFIELD,
                to = ZoneType.GRAVEYARD,
                fromOwnerId = null,
                toOwnerId = "player1"
            )
            transition.leavesBattlefield.shouldBeTrue()
        }

        test("false when not on battlefield") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.HAND,
                to = ZoneType.GRAVEYARD,
                fromOwnerId = "player1",
                toOwnerId = "player1"
            )
            transition.leavesBattlefield.shouldBeFalse()
        }
    }

    context("dies") {
        test("true when moving from battlefield to graveyard") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.BATTLEFIELD,
                to = ZoneType.GRAVEYARD,
                fromOwnerId = null,
                toOwnerId = "player1"
            )
            transition.dies.shouldBeTrue()
        }

        test("false when moving from battlefield to exile") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.BATTLEFIELD,
                to = ZoneType.EXILE,
                fromOwnerId = null,
                toOwnerId = null
            )
            transition.dies.shouldBeFalse()
        }

        test("false when moving from hand to graveyard (discard)") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.HAND,
                to = ZoneType.GRAVEYARD,
                fromOwnerId = "player1",
                toOwnerId = "player1"
            )
            transition.dies.shouldBeFalse()
        }
    }

    context("isDrawn") {
        test("true when moving from library to hand") {
            val transition = ZoneTransition.draw(cardId, cardName, "player1")
            transition.isDrawn.shouldBeTrue()
        }

        test("false when moving from graveyard to hand") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.GRAVEYARD,
                to = ZoneType.HAND,
                fromOwnerId = "player1",
                toOwnerId = "player1"
            )
            transition.isDrawn.shouldBeFalse()
        }
    }

    context("isDiscarded") {
        test("true when moving from hand to graveyard") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.HAND,
                to = ZoneType.GRAVEYARD,
                fromOwnerId = "player1",
                toOwnerId = "player1"
            )
            transition.isDiscarded.shouldBeTrue()
        }
    }

    context("isCast") {
        test("true when moving from hand to stack") {
            val transition = ZoneTransition.cast(cardId, cardName, "player1")
            transition.isCast.shouldBeTrue()
        }
    }

    context("isExiled") {
        test("true when moving to exile from any zone") {
            val transition = ZoneTransition.exile(cardId, cardName, ZoneType.BATTLEFIELD, null)
            transition.isExiled.shouldBeTrue()
        }

        test("false when already in exile") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.EXILE,
                to = ZoneType.EXILE,
                fromOwnerId = null,
                toOwnerId = null
            )
            transition.isExiled.shouldBeFalse()
        }
    }

    context("returnsToHand") {
        test("true when moving from battlefield to hand") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.BATTLEFIELD,
                to = ZoneType.HAND,
                fromOwnerId = null,
                toOwnerId = "player1"
            )
            transition.returnsToHand.shouldBeTrue()
        }

        test("true when moving from graveyard to hand") {
            val transition = ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.GRAVEYARD,
                to = ZoneType.HAND,
                fromOwnerId = "player1",
                toOwnerId = "player1"
            )
            transition.returnsToHand.shouldBeTrue()
        }

        test("false when drawing (library to hand)") {
            val transition = ZoneTransition.draw(cardId, cardName, "player1")
            transition.returnsToHand.shouldBeFalse()
        }
    }

    context("factory methods") {
        test("draw creates correct transition") {
            val transition = ZoneTransition.draw(cardId, cardName, "player1")
            transition.from shouldBe ZoneType.LIBRARY
            transition.to shouldBe ZoneType.HAND
            transition.fromOwnerId shouldBe "player1"
            transition.toOwnerId shouldBe "player1"
        }

        test("cast creates correct transition") {
            val transition = ZoneTransition.cast(cardId, cardName, "player1")
            transition.from shouldBe ZoneType.HAND
            transition.to shouldBe ZoneType.STACK
            transition.fromOwnerId shouldBe "player1"
            transition.toOwnerId shouldBe null
        }

        test("resolve creates correct transition") {
            val transition = ZoneTransition.resolve(cardId, cardName)
            transition.from shouldBe ZoneType.STACK
            transition.to shouldBe ZoneType.BATTLEFIELD
        }

        test("die creates correct transition") {
            val transition = ZoneTransition.die(cardId, cardName, "player1")
            transition.from shouldBe ZoneType.BATTLEFIELD
            transition.to shouldBe ZoneType.GRAVEYARD
            transition.toOwnerId shouldBe "player1"
        }

        test("exile creates correct transition") {
            val transition = ZoneTransition.exile(cardId, cardName, ZoneType.BATTLEFIELD, null)
            transition.from shouldBe ZoneType.BATTLEFIELD
            transition.to shouldBe ZoneType.EXILE
        }
    }
})
