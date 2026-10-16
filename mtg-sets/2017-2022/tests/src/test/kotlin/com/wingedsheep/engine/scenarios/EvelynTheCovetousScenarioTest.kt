package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ExiledFromZoneComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.EvelynTheCovetous
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Evelyn, the Covetous (SNC #184) — {2}{U/B}{B}{B/R} Legendary Creature — Vampire Rogue 2/5.
 *
 * "Flash
 *  Whenever Evelyn or another Vampire you control enters, exile the top card of each player's
 *  library with a collection counter on it.
 *  Once each turn, you may play a card from exile with a collection counter on it if it was exiled
 *  by an ability you controlled, and you may spend mana as though it were mana of any color to
 *  cast it."
 */
class EvelynTheCovetousScenarioTest : FunSpec({

    // {G}{G}: paying it from Swamps proves "as though it were mana of any color".
    val greenBear = card("Collection Test Bear") {
        manaCost = "{G}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    // {C}: "any color" relaxes colored pips only, so a colorless pip still needs colorless mana.
    val colorlessRelic = card("Collection Test Relic") {
        manaCost = "{C}"
        typeLine = "Artifact"
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(EvelynTheCovetous)
        driver.registerCard(greenBear)
        driver.registerCard(colorlessRelic)
        driver.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.legalActionsFor(playerId: EntityId) =
        LegalActionEnumerator.create(cardRegistry).enumerate(state, playerId)

    fun GameTestDriver.canCast(playerId: EntityId, cardId: EntityId): Boolean =
        legalActionsFor(playerId).any { (it.action as? CastSpell)?.cardId == cardId }

    fun GameTestDriver.canAffordCast(playerId: EntityId, cardId: EntityId): Boolean =
        legalActionsFor(playerId).any { (it.action as? CastSpell)?.cardId == cardId && it.affordable }

    fun GameTestDriver.canPlayLand(playerId: EntityId, cardId: EntityId): Boolean =
        legalActionsFor(playerId).any { (it.action as? PlayLand)?.cardId == cardId }

    fun GameTestDriver.collectionCounters(cardId: EntityId): Int =
        state.getEntity(cardId)?.get<CountersComponent>()?.getCount(CounterType.COLLECTION) ?: 0

    fun GameTestDriver.settle() {
        repeat(40) {
            when {
                pendingDecision != null -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> return
            }
        }
    }

    /** A card in [ownerId]'s exile with a collection counter, exiled by an effect [exiledBy] controlled. */
    fun GameTestDriver.putCollectedCard(ownerId: EntityId, cardName: String, exiledBy: EntityId?): EntityId {
        val cardId = putCardInExile(ownerId, cardName)
        addComponent(cardId, CountersComponent().withAdded(CounterType.COLLECTION, 1))
        addComponent(cardId, ExiledFromZoneComponent(Zone.LIBRARY, exiledBy = exiledBy))
        return cardId
    }

    test("Evelyn entering exiles the top card of each player's library with a collection counter") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val myTop = driver.putCardOnTopOfLibrary(me, "Collection Test Relic")
        val theirTop = driver.putCardOnTopOfLibrary(opponent, "Collection Test Bear")
        val evelyn = driver.putCardInHand(me, "Evelyn, the Covetous")
        driver.giveMana(me, Color.BLACK, 5)

        driver.castSpell(me, evelyn).outcome shouldBe Outcome.Done
        driver.settle()

        driver.state.getZone(me, Zone.EXILE).contains(myTop).shouldBeTrue()
        driver.state.getZone(opponent, Zone.EXILE).contains(theirTop).shouldBeTrue()
        driver.collectionCounters(myTop) shouldBe 1
        driver.collectionCounters(theirTop) shouldBe 1
        // Exiled by Evelyn's ability, which I controlled — recorded on the card for the permission.
        driver.state.getEntity(theirTop)?.get<ExiledFromZoneComponent>()?.exiledBy shouldBe me
    }

    test("a collected card can be cast with mana of any color, but only once each turn") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Evelyn, the Covetous")
        val bear = driver.putCollectedCard(opponent, "Collection Test Bear", exiledBy = me)
        val land = driver.putCollectedCard(opponent, "Swamp", exiledBy = me)
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }

        driver.canAffordCast(me, bear).shouldBeTrue()
        driver.canPlayLand(me, land).shouldBeTrue()

        driver.castSpell(me, bear).outcome shouldBe Outcome.Done
        driver.settle()
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(bear).shouldBeTrue()

        // The allowance is spent: the other collected card waits for next turn.
        driver.canPlayLand(me, land).shouldBeFalse()

        // Through the opponent's turn and into my next main phase: the marker is cleared at end of turn.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opponent)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = me)
        driver.canPlayLand(me, land).shouldBeTrue()
    }

    test("playing a collected land spends the same once-each-turn allowance") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Evelyn, the Covetous")
        val land = driver.putCollectedCard(opponent, "Swamp", exiledBy = me)
        val bear = driver.putCollectedCard(opponent, "Collection Test Bear", exiledBy = me)
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }

        driver.playLand(me, land).outcome shouldBe Outcome.Done
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(land).shouldBeTrue()
        driver.canCast(me, bear).shouldBeFalse()
    }

    test("cards exiled by an opponent's ability, or with no collection counter, are not playable") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Evelyn, the Covetous")
        val theirs = driver.putCollectedCard(opponent, "Collection Test Bear", exiledBy = opponent)
        val unattributed = driver.putCollectedCard(opponent, "Collection Test Bear", exiledBy = null)
        val uncountered = driver.putCardInExile(opponent, "Collection Test Bear")
        driver.addComponent(uncountered, ExiledFromZoneComponent(Zone.LIBRARY, exiledBy = me))
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }

        driver.canCast(me, theirs).shouldBeFalse()
        driver.canCast(me, unattributed).shouldBeFalse()
        driver.canCast(me, uncountered).shouldBeFalse()
    }

    test("the permission doesn't depend on which Evelyn exiled the card, but needs one on the battlefield") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val bear = driver.putCollectedCard(opponent, "Collection Test Bear", exiledBy = me)
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }
        driver.canCast(me, bear).shouldBeFalse()

        driver.putCreatureOnBattlefield(me, "Evelyn, the Covetous")
        driver.canCast(me, bear).shouldBeTrue()
    }

    test("mana of any color doesn't pay a colorless pip") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Evelyn, the Covetous")
        val relic = driver.putCollectedCard(opponent, "Collection Test Relic", exiledBy = me)
        driver.putLandOnBattlefield(me, "Swamp")

        // The permission covers it, but a Swamp's black mana can't stand in for {C}.
        driver.canCast(me, relic).shouldBeTrue()
        driver.canAffordCast(me, relic).shouldBeFalse()
    }
})
