package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.WarpExiledComponent
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class WarpMechanicTest : FunSpec({

    val warpCreature = card("Warp Test Creature") {
        manaCost = "{3}{R}{R}"
        typeLine = "Creature — Elemental"
        power = 4
        toughness = 3
        warp = "{1}{R}"
        keywords(Keyword.HASTE)
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(warpCreature))
        return driver
    }

    fun GameTestDriver.gotoMainPhase() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("warp creature can be cast for its warp cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.stackSize shouldBe 1
    }

    test("warped creature enters battlefield with WarpedComponent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Warp Test Creature")
        permanent shouldNotBe null
        driver.state.getEntity(permanent!!)?.has<WarpedComponent>() shouldBe true
    }

    test("warped creature is exiled at beginning of next end step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        driver.findPermanent(player, "Warp Test Creature") shouldNotBe null

        // Advance to end step — delayed trigger should exile it
        driver.passPriorityUntil(Step.END)
        // The delayed trigger fires as a triggered ability on the stack — resolve it
        driver.bothPass()

        // Creature should no longer be on battlefield
        driver.findPermanent(player, "Warp Test Creature") shouldBe null

        // Card should be in exile with WarpExiledComponent
        driver.getExileCardNames(player) shouldBe listOf("Warp Test Creature")
        val exiledCardId = driver.getExile(player).first()
        driver.state.getEntity(exiledCardId)?.has<WarpExiledComponent>() shouldBe true
    }

    test("warped creature can be re-cast from exile at its regular mana cost on a later turn") {
        // CR 702.185a — warp's alternative cost applies only when casting from hand.
        // Re-casting from exile must use the card's regular mana cost.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.bothPass() // Resolve the warp exile trigger

        val exiledCardId = driver.getExile(player).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Warp Test Creature"
        }

        // Advance through end of turn 1 and the opponent's turn 2 to a later turn for player 1.
        driver.passPriorityUntil(Step.END)
        driver.bothPass() // Finish turn 1 → turn 2 (opponent)
        driver.passPriorityUntil(Step.END) // Go through opponent's turn
        driver.bothPass() // Finish turn 2 → turn 3 (player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Pay the FULL mana cost ({3}{R}{R}), not warp cost.
        driver.giveMana(player, Color.RED, 5)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = exiledCardId,
                useAlternativeCost = false,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()
        driver.findPermanent(player, "Warp Test Creature") shouldNotBe null
    }

    test("legal actions on a later turn include a regular-cost cast of the warped exiled card") {
        // CR 702.185a — "Its owner may cast this card after the current turn has ended for
        // as long as it remains exiled." Warp's alt cost is hand-only; from exile the cast
        // must use the regular mana cost. The legal-action enumerator must surface that.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.bothPass() // resolve warp end-step exile trigger

        val exiledCardId = driver.getExile(player).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Warp Test Creature"
        }

        // Advance to player's next main phase
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val legalActions = enumerator.enumerate(driver.state, player)
        val regularCastFromExile = legalActions.firstOrNull { action ->
            action.actionType == "CastSpell" &&
                (action.action as? CastSpell)?.cardId == exiledCardId &&
                (action.action as? CastSpell)?.useAlternativeCost == false
        }
        regularCastFromExile shouldNotBe null
    }

    test("legal actions do NOT offer paying warp cost again from exile") {
        // CR 702.185a — warp's "rather than its mana cost" is hand-only. The UI
        // must not surface a warp-cost cast for cards already exiled by warp.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        val exiledCardId = driver.getExile(player).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Warp Test Creature"
        }

        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val warpCostCastFromExile = enumerator.enumerate(driver.state, player).firstOrNull { action ->
            (action.action as? CastSpell)?.cardId == exiledCardId &&
                (action.action as? CastSpell)?.useAlternativeCost == true
        }
        warpCostCastFromExile shouldBe null
    }

    test("spellWarpedThisTurn is set when a spell is warped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.state.spellWarpedThisTurn shouldBe false

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.state.spellWarpedThisTurn shouldBe true
    }

    test("creature cast for normal cost does not get WarpedComponent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 5)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Warp Test Creature")
        permanent shouldNotBe null
        driver.state.getEntity(permanent!!)?.has<WarpedComponent>() shouldBe false
        driver.state.spellWarpedThisTurn shouldBe false
    }

    test("warped creature killed after re-cast from exile cannot be cast from graveyard") {
        // Bug regression: after warp exile + re-cast from exile, the MayPlayPermission
        // was not cleaned up when the permanent entered the battlefield. When the creature
        // was subsequently killed it ended up in the graveyard while the permission was
        // still active, making it castable from the graveyard.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        // Step 1: cast with warp cost
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        // Step 2: advance to end step — warp exile trigger fires
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        val exiledCardId = driver.getExile(player).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Warp Test Creature"
        }
        driver.state.getEntity(exiledCardId)?.has<WarpExiledComponent>() shouldBe true

        // Step 3: advance to player's next main phase
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Step 4: re-cast from exile at regular cost
        driver.giveMana(player, Color.RED, 5)
        val recastResult = driver.submit(
            CastSpell(
                playerId = player,
                cardId = exiledCardId,
                useAlternativeCost = false,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        recastResult.isSuccess shouldBe true
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Warp Test Creature")
        permanent shouldNotBe null

        // Step 5: kill the creature (send it directly to the graveyard)
        val transitionResult = ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = permanent!!,
            destinationZone = Zone.GRAVEYARD
        )
        driver.replaceState(transitionResult.state)

        driver.findPermanent(player, "Warp Test Creature") shouldBe null
        driver.getGraveyardCardNames(player).contains("Warp Test Creature") shouldBe true

        // Step 6: the creature must NOT be castable from the graveyard
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val legalActions = enumerator.enumerate(driver.state, player)
        val castFromGraveyard = legalActions.firstOrNull { action ->
            (action.action as? CastSpell)?.cardId == permanent &&
                action.sourceZone == "GRAVEYARD"
        }
        castFromGraveyard shouldBe null
    }
})
