package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.TournamentGrounds
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tournament Grounds (ELD #248) — Land.
 *
 *   {T}: Add {C}.
 *   {T}: Add {R}, {W}, or {B}. Spend this mana only to cast a Knight or Equipment spell.
 */
class TournamentGroundsScenarioTest : FunSpec({

    val testKnight = CardDefinition.creature(
        name = "Test White Knight",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Knight")),
        power = 1,
        toughness = 1
    )
    val testEquipment = CardDefinition.artifact(
        name = "Test Black Equipment",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype("Equipment"))
    )
    val testSoldier = CardDefinition.creature(
        name = "Test White Soldier",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TournamentGrounds, testKnight, testEquipment, testSoldier))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        return driver
    }

    val colorlessId = TournamentGrounds.activatedAbilities[0].id
    val whiteId = TournamentGrounds.activatedAbilities[2].id
    val blackId = TournamentGrounds.activatedAbilities[3].id

    fun GameTestDriver.tapGrounds(playerId: EntityId, abilityId: com.wingedsheep.sdk.scripting.AbilityId) {
        val grounds = putLandOnBattlefield(playerId, "Tournament Grounds")
        submit(ActivateAbility(playerId, grounds, abilityId)).error shouldBe null
    }

    fun GameTestDriver.castFromPool(playerId: EntityId, name: String) =
        submit(CastSpell(playerId = playerId, cardId = putCardInHand(playerId, name), paymentStrategy = PaymentStrategy.FromPool))

    test("the colorless ability adds unrestricted {C}") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapGrounds(p1, colorlessId)

        val pool = driver.state.getEntity(p1)?.get<ManaPoolComponent>()!!
        pool.colorless shouldBe 1
        pool.restrictedMana.size shouldBe 0
    }

    test("the white mana pays for a Knight spell") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapGrounds(p1, whiteId)
        val pool = driver.state.getEntity(p1)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.single().color shouldBe Color.WHITE

        driver.castFromPool(p1, "Test White Knight").outcome shouldBe Outcome.Done
    }

    test("the black mana pays for an Equipment spell") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapGrounds(p1, blackId)

        driver.castFromPool(p1, "Test Black Equipment").outcome shouldBe Outcome.Done
    }

    test("the coloured mana can't pay for a non-Knight, non-Equipment spell") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapGrounds(p1, whiteId)

        driver.castFromPool(p1, "Test White Soldier").outcome shouldNotBe Outcome.Done
    }
})
