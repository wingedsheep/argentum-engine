package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `AbilityCost.PutSelfIntoLibrary` — "Put this card from your graveyard on the bottom (or top) of
 * your library" as an activation cost. The card moves as part of paying the cost, so it has left
 * the graveyard before the ability is on the stack, and it lands at the printed position.
 */
class PutSelfIntoLibraryCostTest : FunSpec({

    val bottomCard = card("Bottom Recycler") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
        oracleText = "{1}, Put this card from your graveyard on the bottom of your library: You gain 2 life."
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{1}"), Costs.PutSelfOnBottomOfLibrary)
            effect = Effects.GainLife(2)
            activateFromZone = Zone.GRAVEYARD
        }
    }

    val topCard = card("Top Recycler") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
        oracleText = "Put this card from your graveyard on top of your library: You gain 1 life."
        activatedAbility {
            cost = Costs.PutSelfOnTopOfLibrary
            effect = Effects.GainLife(1)
            activateFromZone = Zone.GRAVEYARD
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(bottomCard)
        driver.registerCard(topCard)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("bottom: the card is on the bottom of the library before the ability resolves") {
        val driver = createDriver()
        val card = driver.putCardInGraveyard(driver.player1, "Bottom Recycler")
        driver.giveMana(driver.player1, Color.BLACK, 1)
        val life = driver.getLifeTotal(driver.player1)
        val abilityId = driver.cardRegistry.requireCard("Bottom Recycler").activatedAbilities[0].id

        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = card, abilityId = abilityId))

        (card in driver.getGraveyard(driver.player1)) shouldBe false
        driver.state.getLibrary(driver.player1).last() shouldBe card
        driver.state.stack.size shouldBe 1

        driver.bothPass()
        driver.getLifeTotal(driver.player1) shouldBe life + 2
    }

    test("top: the card goes on top of the library") {
        val driver = createDriver()
        val card = driver.putCardInGraveyard(driver.player1, "Top Recycler")
        val abilityId = driver.cardRegistry.requireCard("Top Recycler").activatedAbilities[0].id

        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = card, abilityId = abilityId))

        driver.state.getLibrary(driver.player1).first() shouldBe card
    }

    test("the cost can't be paid twice — once the card has left the graveyard the ability is gone") {
        val driver = createDriver()
        val card = driver.putCardInGraveyard(driver.player1, "Top Recycler")
        val abilityId = driver.cardRegistry.requireCard("Top Recycler").activatedAbilities[0].id

        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = card, abilityId = abilityId))
        (driver.submit(ActivateAbility(playerId = driver.player1, sourceId = card, abilityId = abilityId)).error != null) shouldBe true
        driver.state.stack.size shouldBe 1
    }

    test("only top or bottom are meaningful placements") {
        shouldThrow<IllegalArgumentException> { AbilityCost.PutSelfIntoLibrary(ZonePlacement.Shuffled) }
    }
})
