package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.TheKeyToTheVault
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Key to the Vault — {1}{U} Legendary Artifact — Equipment.
 *
 * "Whenever equipped creature deals combat damage to a player, look at that many cards from the
 * top of your library. You may exile a nonland card from among them. Put the rest on the bottom
 * of your library in a random order. You may cast the exiled card without paying its mana cost.
 * Equip {2}{U}"
 */
class OtjTheKeyToTheVaultScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TheKeyToTheVault)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * Equip the Key to [attacker], attack the opponent unblocked, and pass priority until the
     * Key's combat-damage trigger pauses for the look/exile selection. Returns the look decision.
     */
    fun setupAndAttack(driver: GameTestDriver, me: EntityId, opp: EntityId): EntityId {
        val attacker = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        driver.removeSummoningSickness(attacker)
        val key = driver.putPermanentOnBattlefield(me, "The Key to the Vault")

        // Equip the Key onto the attacker (sorcery-speed, our main phase, empty stack).
        val equipId = TheKeyToTheVault.activatedAbilities.first().id
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.BLUE, 1) // {2}{U}
        driver.submit(
            ActivateAbility(me, key, equipId, targets = listOf(ChosenTarget.Permanent(attacker)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(key)?.get<AttachedToComponent>()?.targetId shouldBe attacker

        // Move to our declare-attackers step and swing unblocked.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opp)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opp)

        // Pass until the combat-damage trigger pauses for the look/exile choice.
        var safety = 0
        while (!driver.isPaused && safety++ < 30) driver.bothPass()
        return attacker
    }

    test("combat damage triggers a look at N, exiles a nonland, and casts it for free") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // A castable nonland on top, so the exiled card has a legal free cast.
        val lions = driver.putCardOnTopOfLibrary(me, "Savannah Lions")

        setupAndAttack(driver, me, opp)

        driver.isPaused shouldBe true
        val look = driver.pendingDecision
        look.shouldBeInstanceOf<SelectCardsDecision>()

        // Exile Savannah Lions, then it is cast for free during the trigger's resolution.
        driver.submitDecision(
            me,
            CardsSelectedResponse(decisionId = (look as SelectCardsDecision).id, selectedCards = listOf(lions))
        )
        var safety = 0
        while (driver.isPaused && safety++ < 10) driver.bothPass()
        driver.bothPass()

        // Savannah Lions was cast for free and is now on the battlefield.
        driver.findPermanent(me, "Savannah Lions") shouldBe lions
    }

    test("the look trigger may decline to exile, leaving nothing cast") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val lions = driver.putCardOnTopOfLibrary(me, "Savannah Lions")

        setupAndAttack(driver, me, opp)

        driver.isPaused shouldBe true
        val look = driver.pendingDecision
        look.shouldBeInstanceOf<SelectCardsDecision>()

        // Decline to exile anything.
        driver.submitDecision(
            me,
            CardsSelectedResponse(decisionId = (look as SelectCardsDecision).id, selectedCards = emptyList<EntityId>())
        )
        var safety = 0
        while (driver.isPaused && safety++ < 10) driver.bothPass()
        driver.bothPass()

        // Nothing was cast and Savannah Lions was not exiled.
        driver.findPermanent(me, "Savannah Lions") shouldBe null
        driver.getExile(me).none {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Savannah Lions"
        } shouldBe true
    }
})
