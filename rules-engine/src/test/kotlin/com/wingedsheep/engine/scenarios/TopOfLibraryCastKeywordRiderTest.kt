package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [CastSpellTypesFromTopOfLibrary.castThisWayKeyword] — the cast-this-way keyword rider of a
 * top-of-library cast permission ("If you cast a creature spell this way, it gains haste until end
 * of turn"). The rider belongs to the permission, so it reaches only a spell that permission could
 * have authorized; a card cast off the top through a different permission alone gets nothing.
 */
class TopOfLibraryCastKeywordRiderTest : FunSpec({

    val hasteGranter = CardDefinition.creature(
        name = "Test Haste Granter",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
        script = CardScript(
            staticAbilities = listOf(
                CastSpellTypesFromTopOfLibrary(
                    filter = GameObjectFilter.Creature.powerAtLeast(4),
                    castThisWayKeyword = Keyword.HASTE,
                )
            )
        ),
    )
    val futureSight = CardDefinition.creature(
        name = "Test Future Sight",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
        script = CardScript(staticAbilities = listOf(PlayFromTopOfLibrary)),
    )
    val citadel = CardDefinition.creature(
        name = "Test Citadel",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
        script = CardScript(
            staticAbilities = listOf(
                PlayFromTopWithAlternativeCost(
                    withoutPayingManaCost = true,
                    additionalCost = AdditionalCost.PayLifeEqualToManaValueOfSpell,
                )
            )
        ),
    )
    val big = CardDefinition.creature("Test Big", ManaCost.parse("{1}"), emptySet(), 4, 4)
    val small = CardDefinition.creature("Test Small", ManaCost.parse("{1}"), emptySet(), 2, 2)

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(hasteGranter, futureSight, citadel, big, small))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(you, "Test Haste Granter")
        driver.putCreatureOnBattlefield(you, "Test Future Sight")
        return driver to you
    }

    fun castFromTop(driver: GameTestDriver, you: EntityId, name: String): EntityId {
        val top = driver.putCardOnTopOfLibrary(you, name)
        driver.giveColorlessMana(you, 1)
        driver.submit(CastSpell(you, top, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        driver.bothPass()
        driver.state.getBattlefield(you).contains(top) shouldBe true
        return top
    }

    test("a spell the rider-bearing permission covers gains the keyword even when another permission also covers it") {
        val (driver, you) = setup()
        val top = castFromTop(driver, you, "Test Big")
        driver.state.projectedState.hasKeyword(top, Keyword.HASTE) shouldBe true
    }

    test("a spell cast off the top only through another permission gains nothing") {
        val (driver, you) = setup()
        val top = castFromTop(driver, you, "Test Small")
        driver.state.projectedState.hasKeyword(top, Keyword.HASTE) shouldBe false
    }

    test("a cast the engine routes through a payment-changing grant gains nothing") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(hasteGranter, citadel, big))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(you, "Test Haste Granter")
        driver.putCreatureOnBattlefield(you, "Test Citadel")
        val lifeBefore = driver.getLifeTotal(you)
        val top = driver.putCardOnTopOfLibrary(you, "Test Big")
        driver.submit(CastSpell(you, top, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        driver.bothPass()
        driver.state.getBattlefield(you).contains(top) shouldBe true
        driver.getLifeTotal(you) shouldBe lifeBefore - 1
        driver.state.projectedState.hasKeyword(top, Keyword.HASTE) shouldBe false
    }
})
