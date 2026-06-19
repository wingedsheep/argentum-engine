package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownTurnUpComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ktk.cards.RattleclawMystic
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Rattleclaw Mystic:
 * {1}{G} Creature — Human Shaman 2/1
 * {T}: Add {G}, {U}, or {R}.
 * Morph {2}
 * When Rattleclaw Mystic is turned face up, add {G}{U}{R}.
 */
class RattleclawMysticTest : FunSpec({

    val allCards = TestCards.all + listOf(RattleclawMystic)
    val greenAbilityId = RattleclawMystic.activatedAbilities[0].id
    val blueAbilityId = RattleclawMystic.activatedAbilities[1].id
    val redAbilityId = RattleclawMystic.activatedAbilities[2].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    fun getManaPool(driver: GameTestDriver, playerId: EntityId): ManaPoolComponent {
        return driver.state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        val morphAbility = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Morph>()
            .firstOrNull()
        replaceState(state.updateEntity(creatureId) { container ->
            var c = container.with(FaceDownComponent)
            if (morphAbility != null) {
                c = c.with(FaceDownTurnUpComponent(morphAbility.morphCost, cardDef.name))
            }
            c
        })
        return creatureId
    }

    test("tap ability adds green mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mystic = driver.putCreatureOnBattlefield(activePlayer, "Rattleclaw Mystic")
        driver.removeSummoningSickness(mystic)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mystic,
                abilityId = greenAbilityId
            )
        )
        result.isSuccess shouldBe true

        val pool = getManaPool(driver, activePlayer)
        pool.green shouldBe 1
    }

    test("tap ability adds blue mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mystic = driver.putCreatureOnBattlefield(activePlayer, "Rattleclaw Mystic")
        driver.removeSummoningSickness(mystic)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mystic,
                abilityId = blueAbilityId
            )
        )
        result.isSuccess shouldBe true

        val pool = getManaPool(driver, activePlayer)
        pool.blue shouldBe 1
    }

    test("tap ability adds red mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mystic = driver.putCreatureOnBattlefield(activePlayer, "Rattleclaw Mystic")
        driver.removeSummoningSickness(mystic)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mystic,
                abilityId = redAbilityId
            )
        )
        result.isSuccess shouldBe true

        val pool = getManaPool(driver, activePlayer)
        pool.red shouldBe 1
    }

    test("can be cast face down for 3 generic mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mysticCard = driver.putCardInHand(activePlayer, "Rattleclaw Mystic")
        driver.giveMana(activePlayer, Color.GREEN, 3)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = mysticCard,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.stackSize shouldBe 1
    }

    test("turning face up adds GUR mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put face-down creature on battlefield
        val mystic = driver.putFaceDownCreature(activePlayer, "Rattleclaw Mystic")
        driver.removeSummoningSickness(mystic)

        // Give {2} for morph cost
        driver.giveMana(activePlayer, Color.GREEN, 2)

        val result = driver.submit(
            TurnFaceUp(
                playerId = activePlayer,
                sourceId = mystic,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        // Resolve the turn-face-up triggered ability
        driver.bothPass()

        // Should have {G}{U}{R} from turn-face-up trigger
        val pool = getManaPool(driver, activePlayer)
        pool.green shouldBe 1
        pool.blue shouldBe 1
        pool.red shouldBe 1
    }
})
