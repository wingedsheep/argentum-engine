package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.HandLookedAtEvent
import com.wingedsheep.engine.core.LookedAtCardsEvent
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.targeting.TargetPlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Spy Network.
 *
 * Spy Network: {U}
 * Instant
 * Look at target player's hand, the top card of that player's library, and any
 * face-down creatures they control. Look at the top four cards of your library,
 * then put them back in any order.
 */
class SpyNetworkTest : FunSpec({

    val SpyNetwork = CardDefinition(
        name = "Spy Network",
        manaCost = ManaCost.parse("{U}"),
        typeLine = TypeLine.instant(),
        oracleText = "Look at target player's hand, the top card of that player's library, and any face-down creatures they control. Look at the top four cards of your library, then put them back in any order.",
        script = CardScript.spell(
            effect = CompositeEffect(
                listOf(
                    LookAtTargetHandEffect(EffectTarget.ContextTarget(0)),
                    CompositeEffect(listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.ContextPlayer(0)),
                            storeAs = "target_top"
                        ),
                        MoveCollectionEffect(
                            from = "target_top",
                            destination = CardDestination.ToZone(Zone.LIBRARY, Player.ContextPlayer(0), ZonePlacement.Top),
                            order = CardOrder.ControllerChooses
                        )
                    )),
                    LookAtAllFaceDownCreaturesEffect(EffectTarget.ContextTarget(0)),
                    EffectPatterns.lookAtTopAndReorder(4)
                )
            ),
            TargetPlayer()
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SpyNetwork))
        return driver
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        replaceState(state.updateEntity(creatureId) { it.with(FaceDownComponent) })
        return creatureId
    }

    test("Spy Network reveals opponent's hand, top card of their library, face-down creatures, and lets you reorder top 4") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Set up: put a card in opponent's hand
        val opponentHandCard = driver.putCardInHand(opponent, "Grizzly Bears")

        // Set up: put a face-down creature on opponent's battlefield
        val faceDownCreature = driver.putFaceDownCreature(opponent, "Grizzly Bears")

        // Give mana and Spy Network
        val spyNetwork = driver.putCardInHand(activePlayer, "Spy Network")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Cast Spy Network targeting opponent
        driver.castSpell(activePlayer, spyNetwork, targets = listOf(opponent))

        // Resolve spell
        driver.bothPass()

        // 1) Opponent's hand should be revealed to us
        val handLookedAtEvents = driver.events.filterIsInstance<HandLookedAtEvent>()
        handLookedAtEvents.size shouldBe 1
        handLookedAtEvents[0].viewingPlayerId shouldBe activePlayer
        handLookedAtEvents[0].targetPlayerId shouldBe opponent

        val handCardRevealed = driver.state.getEntity(opponentHandCard)?.get<RevealedToComponent>()
        handCardRevealed shouldNotBe null
        handCardRevealed!!.isRevealedTo(activePlayer) shouldBe true

        // 2) Should get a reorder decision for opponent's top 1 card
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        val opponentTopDecision = driver.pendingDecision as ReorderLibraryDecision
        opponentTopDecision.cards.size shouldBe 1
        driver.submitOrderedResponse(activePlayer, opponentTopDecision.cards)

        // 3) Face-down creature should be revealed to us
        val faceDownRevealed = driver.state.getEntity(faceDownCreature)?.get<RevealedToComponent>()
        faceDownRevealed shouldNotBe null
        faceDownRevealed!!.isRevealedTo(activePlayer) shouldBe true

        val lookedAtEvents = driver.events.filterIsInstance<LookedAtCardsEvent>()
        lookedAtEvents.any { it.cardIds.contains(faceDownCreature) } shouldBe true

        // 4) Should get a reorder decision for our top 4 cards
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        val ownTopDecision = driver.pendingDecision as ReorderLibraryDecision
        ownTopDecision.cards.size shouldBe 4
        driver.submitOrderedResponse(activePlayer, ownTopDecision.cards)

        // Should be fully resolved now
        driver.isPaused shouldBe false
    }

    test("Spy Network works when opponent has no face-down creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spyNetwork = driver.putCardInHand(activePlayer, "Spy Network")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        driver.castSpell(activePlayer, spyNetwork, targets = listOf(opponent))
        driver.bothPass()

        // Hand reveal event should fire
        val handLookedAtEvents = driver.events.filterIsInstance<HandLookedAtEvent>()
        handLookedAtEvents.size shouldBe 1

        // Reorder opponent's top 1 card
        driver.isPaused shouldBe true
        val opponentTopDecision = driver.pendingDecision as ReorderLibraryDecision
        driver.submitOrderedResponse(activePlayer, opponentTopDecision.cards)

        // No face-down creatures, so goes straight to own library reorder
        driver.isPaused shouldBe true
        val ownTopDecision = driver.pendingDecision as ReorderLibraryDecision
        ownTopDecision.cards.size shouldBe 4
        driver.submitOrderedResponse(activePlayer, ownTopDecision.cards)

        driver.isPaused shouldBe false
    }

    test("Spy Network can target yourself") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spyNetwork = driver.putCardInHand(activePlayer, "Spy Network")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Target yourself
        driver.castSpell(activePlayer, spyNetwork, targets = listOf(activePlayer))
        driver.bothPass()

        // Hand reveal event should fire (looking at own hand)
        val handLookedAtEvents = driver.events.filterIsInstance<HandLookedAtEvent>()
        handLookedAtEvents.size shouldBe 1
        handLookedAtEvents[0].targetPlayerId shouldBe activePlayer

        // Reorder own top 1 card (from target player's library)
        driver.isPaused shouldBe true
        val targetTopDecision = driver.pendingDecision as ReorderLibraryDecision
        driver.submitOrderedResponse(activePlayer, targetTopDecision.cards)

        // Reorder own top 4 cards (from controller's library)
        driver.isPaused shouldBe true
        val ownTopDecision = driver.pendingDecision as ReorderLibraryDecision
        ownTopDecision.cards.size shouldBe 4
        driver.submitOrderedResponse(activePlayer, ownTopDecision.cards)

        driver.isPaused shouldBe false
    }
})
