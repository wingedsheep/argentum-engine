package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.BristlebaneBattler
import com.wingedsheep.mtg.sets.definitions.ecl.cards.ChampionOfTheClachan
import com.wingedsheep.mtg.sets.definitions.ecl.cards.FlockImpostor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Repro for: when Champion of the Clachan leaves the battlefield because it was
 * bounced to hand (e.g. Flock Impostor's ETB), the beheld-and-exiled Kithkin must
 * still return to its owner's hand from exile.
 *
 * Previously the LinkedExileComponent was only re-attached on graveyard/exile
 * destinations during the battlefield exit, so a hand bounce silently dropped
 * the linked exile reference and the LTB trigger had nothing to return.
 */
class ChampionOfTheClachanBouncedTest : FunSpec({

    test("Champion bounced to hand by Flock Impostor returns its beheld Kithkin from exile") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ChampionOfTheClachan + BristlebaneBattler + FlockImpostor)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)

        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Champion of the Clachan on P1's battlefield, linked to a Bristlebane Battler in P1's exile.
        // (This is the post-cast-time state after paying "behold a Kithkin and exile it".)
        val champion = driver.putCreatureOnBattlefield(p1, "Champion of the Clachan")
        val bristlebane = createCardInExile(driver, p1, "Bristlebane Battler")
        driver.replaceState(driver.state.updateEntity(champion) { c ->
            c.with(LinkedExileComponent(listOf(bristlebane)))
        })

        // Sanity check: Bristlebane sits in P1's exile, linked to Champion.
        driver.getExile(p1) shouldBe listOf(bristlebane)
        driver.state.getEntity(champion)?.get<LinkedExileComponent>()?.exiledIds shouldBe listOf(bristlebane)

        // P1 casts Flock Impostor; its ETB returns Champion to hand.
        val impostor = driver.putCardInHand(p1, "Flock Impostor")
        driver.giveMana(p1, Color.WHITE, 3)
        driver.castSpell(p1, impostor)
        driver.bothPass()                                   // Flock Impostor resolves → ETB trigger queues
        driver.submitTargetSelection(p1, listOf(champion))  // bounce Champion
        driver.bothPass()                                   // resolve ETB → Champion → hand → LTB trigger queues
        driver.bothPass()                                   // resolve Champion's LTB trigger

        // Champion is in P1's hand; Bristlebane has been returned from exile to P1's hand.
        driver.findPermanent(p1, "Champion of the Clachan") shouldBe null
        driver.findCardsInHand(p1, "Champion of the Clachan").size shouldBe 1
        driver.getExile(p1).any { driver.state.getEntity(it)?.get<CardComponent>()?.name == "Bristlebane Battler" } shouldBe false
        driver.findCardsInHand(p1, "Bristlebane Battler").size shouldBe 1
    }
})

private fun createCardInExile(driver: GameTestDriver, ownerId: EntityId, cardName: String): EntityId {
    val cardDef = driver.cardRegistry.requireCard(cardName)
    val cardId = EntityId.generate()
    val cardComponent = CardComponent(
        cardDefinitionId = cardDef.name,
        name = cardDef.name,
        manaCost = cardDef.manaCost,
        typeLine = cardDef.typeLine,
        oracleText = cardDef.oracleText,
        baseStats = cardDef.creatureStats,
        baseKeywords = cardDef.keywords,
        baseFlags = cardDef.flags,
        colors = cardDef.colors,
        ownerId = ownerId,
        spellEffect = cardDef.spellEffect,
    )
    val container = ComponentContainer.of(
        cardComponent,
        OwnerComponent(ownerId),
        ControllerComponent(ownerId)
    )
    val newState = driver.state.withEntity(cardId, container).addToZone(ZoneKey(ownerId, Zone.EXILE), cardId)
    driver.replaceState(newState)
    return cardId
}
