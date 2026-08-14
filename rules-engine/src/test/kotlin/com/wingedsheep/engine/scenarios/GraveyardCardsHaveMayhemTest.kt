package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveMayhem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * `GraveyardCardsHaveMayhem` — the whole-graveyard Mayhem group grant behind Green Goblin's "Goblin
 * Formula — Each nonland card in your graveyard has mayhem. The mayhem cost is equal to its mana
 * cost." Pins that a battlefield static grants Mayhem to matching graveyard cards (still gated on the
 * discarded-this-turn requirement), routed through `MayhemGrants.effectiveMayhem`.
 */
class GraveyardCardsHaveMayhemTest : FunSpec({

    // A minimal battlefield granter — "Each nonland card in your graveyard has mayhem (= mana cost)".
    val goblinFormula = card("Goblin Formula Test") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        staticAbility { ability = GraveyardCardsHaveMayhem(filter = GameObjectFilter.Nonland) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(goblinFormula))
        return driver
    }

    fun markDiscarded(driver: GameTestDriver, playerId: EntityId, cardId: EntityId) {
        val prior = driver.state.getEntity(playerId)?.get<CardsDiscardedThisTurnComponent>()
            ?: CardsDiscardedThisTurnComponent()
        driver.addComponent(playerId, prior.copy(cardIds = prior.cardIds + cardId, count = prior.count + 1))
    }

    fun mayhemActionCardIds(driver: GameTestDriver, playerId: EntityId) =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.alternativeCostType == AlternativeCostType.MAYHEM }
            .map { it.cardId }

    test("a graveyard static grants mayhem to a discarded nonland card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Goblin Formula Test")
        // Lightning Bolt has no printed mayhem; the grant supplies it (cost = its mana cost, {R}).
        val bolt = driver.putCardInGraveyard(you, "Lightning Bolt")
        markDiscarded(driver, you, bolt)
        driver.giveMana(you, Color.RED, 1)

        mayhemActionCardIds(driver, you) shouldContain bolt
    }

    test("no granter → no mayhem for a plain graveyard card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bolt = driver.putCardInGraveyard(you, "Lightning Bolt")
        markDiscarded(driver, you, bolt)
        driver.giveMana(you, Color.RED, 1)

        (bolt in mayhemActionCardIds(driver, you)) shouldBe false
    }

    test("the Nonland filter excludes a discarded land") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Goblin Formula Test")
        val bolt = driver.putCardInGraveyard(you, "Lightning Bolt")
        markDiscarded(driver, you, bolt)
        val land = driver.putCardInGraveyard(you, "Mountain")
        markDiscarded(driver, you, land)
        driver.giveMana(you, Color.RED, 1)

        val ids = mayhemActionCardIds(driver, you)
        ids shouldContain bolt        // nonland card gains mayhem
        (land in ids) shouldBe false  // the land is excluded by GameObjectFilter.Nonland
    }

    test("granted mayhem still requires the card to have been discarded this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Goblin Formula Test")
        val bolt = driver.putCardInGraveyard(you, "Lightning Bolt") // NOT discarded this turn
        driver.giveMana(you, Color.RED, 1)

        (bolt in mayhemActionCardIds(driver, you)) shouldBe false
    }
})
