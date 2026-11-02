package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.PermanentPutIntoZoneFromBattlefieldThisTurn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for `PermanentPutIntoZoneFromBattlefieldThisTurn` — owner-keyed turn history of permanents
 * put into a player's graveyard or hand from the battlefield (Kami of Transience's "put into your
 * graveyard", Miles O'Brien's "returned to your hand").
 */
class PermanentPutIntoZoneFromBattlefieldThisTurnConditionTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.eval(player: EntityId, condition: PermanentPutIntoZoneFromBattlefieldThisTurn): Boolean {
        val context = EffectContext(sourceId = null, controllerId = player, targets = emptyList(), xValue = 0)
        return PredicateEvaluator(cardRegistry = null).conditions.evaluate(state, condition, context)
    }

    fun GameTestDriver.move(entity: EntityId, zone: Zone) {
        replaceState(zones.moveToZone(state = state, entityId = entity, destinationZone = zone).state)
    }

    val toHand = PermanentPutIntoZoneFromBattlefieldThisTurn(Zone.HAND)
    val toGraveyard = PermanentPutIntoZoneFromBattlefieldThisTurn(Zone.GRAVEYARD)

    test("a permanent returned to hand is recorded for HAND, not GRAVEYARD") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.eval(player, toHand) shouldBe false

        driver.move(driver.putCreatureOnBattlefield(player, "Grizzly Bears"), Zone.HAND)

        driver.eval(player, toHand) shouldBe true
        driver.eval(player, toGraveyard) shouldBe false
    }

    test("keyed on the owner: an opponent's card you controlled goes to their hand, not yours") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stolen = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.replaceState(driver.state.updateEntity(stolen) { it.with(ControllerComponent(player)) })
        driver.move(stolen, Zone.HAND)

        driver.eval(player, toHand) shouldBe false
        driver.eval(opponent, toHand) shouldBe true
    }

    test("a token counts: it reaches the hand before ceasing to exist") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val token = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.replaceState(driver.state.updateEntity(token) { it.with(TokenComponent) })
        driver.move(token, Zone.HAND)

        driver.eval(player, toHand) shouldBe true
    }

    test("the card-type axis filters on the last-known type line") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.move(driver.putCreatureOnBattlefield(player, "Grizzly Bears"), Zone.HAND)

        driver.eval(player, PermanentPutIntoZoneFromBattlefieldThisTurn(Zone.HAND, CardType.CREATURE)) shouldBe true
        driver.eval(player, PermanentPutIntoZoneFromBattlefieldThisTurn(Zone.HAND, CardType.ARTIFACT)) shouldBe false
    }

    test("a card moved from another zone into hand doesn't count") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.move(bears, Zone.GRAVEYARD)
        driver.move(bears, Zone.HAND)

        driver.eval(player, toGraveyard) shouldBe true
        driver.eval(player, toHand) shouldBe false
    }

    test("resets at end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.move(bears, Zone.HAND)
        driver.eval(player, toHand) shouldBe true
        // Keep the hand at seven so cleanup doesn't pause for a discard.
        driver.move(bears, Zone.LIBRARY)

        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.activePlayer shouldBe driver.getOpponent(player)
        driver.eval(player, toHand) shouldBe false
    }

    test("only graveyard and hand are tracked destinations") {
        shouldThrow<IllegalArgumentException> { PermanentPutIntoZoneFromBattlefieldThisTurn(Zone.EXILE) }
    }

    test("description reads like the printed text") {
        toHand.description shouldBe "if a permanent was returned to your hand from the battlefield this turn"
        PermanentPutIntoZoneFromBattlefieldThisTurn(Zone.GRAVEYARD, CardType.ENCHANTMENT).description shouldBe
            "if an Enchantment was put into your graveyard from the battlefield this turn"
    }
})
