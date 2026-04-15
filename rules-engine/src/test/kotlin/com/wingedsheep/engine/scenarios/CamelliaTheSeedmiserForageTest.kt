package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.CamelliaTheSeedmiser
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain

class CamelliaTheSeedmiserForageTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CamelliaTheSeedmiser))
        return driver
    }

    val abilityId = CamelliaTheSeedmiser.activatedAbilities.first().id

    test("Forage ability advertises graveyard exile targets so player can pick which 3 to exile") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val camellia = driver.putCreatureOnBattlefield(active, "Camellia, the Seedmiser")
        driver.replaceState(
            driver.state.updateEntity(camellia) { it.without<SummoningSicknessComponent>() }
        )
        // Five cards in graveyard — player must be able to pick which 3 to exile.
        val graveCards = (1..5).map { driver.putCardInGraveyard(active, "Forest") }
        // Enough mana on battlefield to pay {2}
        repeat(3) { driver.putLandOnBattlefield(active, "Forest") }

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, active, EnumerationMode.FULL)
        val forage = actions.single {
            val a = it.action
            a is ActivateAbility && a.sourceId == camellia && a.abilityId == abilityId
        }

        forage.affordable shouldBe true
        val cost = forage.additionalCostInfo!!
        cost.costType shouldBe "ExileFromGraveyard"
        cost.exileMinCount shouldBe 3
        cost.exileMaxCount shouldBe 3
        cost.validExileTargets shouldContainAll graveCards
    }

    test("Paying forage with player-chosen exile cards exiles exactly those cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val camellia = driver.putCreatureOnBattlefield(active, "Camellia, the Seedmiser")
        driver.replaceState(
            driver.state.updateEntity(camellia) { it.without<SummoningSicknessComponent>() }
        )
        val a = driver.putCardInGraveyard(active, "Forest")
        val b = driver.putCardInGraveyard(active, "Forest")
        val c = driver.putCardInGraveyard(active, "Forest")
        val d = driver.putCardInGraveyard(active, "Forest")
        val e = driver.putCardInGraveyard(active, "Forest")
        repeat(3) { driver.putLandOnBattlefield(active, "Forest") }

        val chosen = listOf(b, d, e)
        val result = driver.submit(
            ActivateAbility(
                playerId = active,
                sourceId = camellia,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = chosen)
            )
        )
        result.isSuccess shouldBe true

        val exile = driver.state.getExile(active)
        exile shouldContainAll chosen
        exile shouldNotContain a
        exile shouldNotContain c
        driver.state.getGraveyard(active) shouldContain a
        driver.state.getGraveyard(active) shouldContain c
    }
})
