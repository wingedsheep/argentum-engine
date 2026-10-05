package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.VialOfDragonfire
import com.wingedsheep.mtg.sets.definitions.frf.cards.RenownedWeaponsmith
import com.wingedsheep.mtg.sets.definitions.ktk.cards.HeartPiercerBow
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
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Renowned Weaponsmith (FRF #48) — {1}{U} Creature — Human Artificer 1/3
 *
 *   {T}: Add {C}{C}. Spend this mana only to cast artifact spells or activate abilities of artifacts.
 *   {U}, {T}: Search your library for a card named Heart-Piercer Bow or Vial of Dragonfire, reveal it,
 *   put it into your hand, then shuffle.
 *
 *  1. Tapping adds two restricted colorless mana, which pays for an artifact spell (Vial of Dragonfire).
 *  2. That mana can't pay for a non-artifact spell of the same cost.
 *  3. The tutor offers only the two named cards, and the chosen one goes to hand.
 */
class RenownedWeaponsmithScenarioTest : FunSpec({

    val testCreature = CardDefinition.creature(
        name = "Test Two-Drop Creature",
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype("Human")),
        power = 2,
        toughness = 2
    )

    val manaAbilityId = RenownedWeaponsmith.activatedAbilities[0].id
    val tutorAbilityId = RenownedWeaponsmith.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(RenownedWeaponsmith, VialOfDragonfire, HeartPiercerBow, testCreature)
        )
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.readyWeaponsmith(playerId: EntityId): EntityId {
        val smith = putCreatureOnBattlefield(playerId, "Renowned Weaponsmith")
        removeSummoningSickness(smith)
        return smith
    }

    test("tapping adds two restricted colorless mana that pays for an artifact spell") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val smith = driver.readyWeaponsmith(you)

        driver.submit(ActivateAbility(you, smith, manaAbilityId)).error shouldBe null

        val pool = driver.state.getEntity(you)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 2
        pool.restrictedMana.all { it.color == null } shouldBe true

        val vial = driver.putCardInHand(you, "Vial of Dragonfire")
        driver.submit(
            CastSpell(playerId = you, cardId = vial, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
    }

    test("the mana can't pay for a non-artifact spell") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val smith = driver.readyWeaponsmith(you)

        driver.submit(ActivateAbility(you, smith, manaAbilityId)).error shouldBe null

        val creature = driver.putCardInHand(you, "Test Two-Drop Creature")
        driver.submit(
            CastSpell(playerId = you, cardId = creature, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldNotBe Outcome.Done
    }

    test("the tutor offers only the two named cards and puts the chosen one into hand") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val smith = driver.readyWeaponsmith(you)

        val bow = driver.putCardOnTopOfLibrary(you, "Heart-Piercer Bow")
        val vial = driver.putCardOnTopOfLibrary(you, "Vial of Dragonfire")
        driver.giveMana(you, Color.BLUE, 1)

        driver.submit(ActivateAbility(you, smith, tutorAbilityId)).outcome shouldBe Outcome.Done
        driver.isTapped(smith) shouldBe true

        driver.bothPass()
        val search = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.options.toSet() shouldBe setOf(bow, vial)
        driver.submitCardSelection(you, listOf(bow))

        driver.getHand(you).contains(bow) shouldBe true
        driver.getHand(you).contains(vial) shouldBe false
    }
})
