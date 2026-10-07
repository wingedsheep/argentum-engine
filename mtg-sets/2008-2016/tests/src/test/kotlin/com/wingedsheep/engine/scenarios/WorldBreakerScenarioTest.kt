package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.WorldBreaker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class WorldBreakerScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(WorldBreaker)
        initMirrorMatch(deck = Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("cast trigger exiles a land before the creature resolves") {
        val game = driver()
        val you = game.activePlayer!!
        val opponent = game.getOpponent(you)
        val breaker = game.putCardInHand(you, "World Breaker")
        val land = game.putLandOnBattlefield(opponent, "Forest")
        game.giveMana(you, Color.GREEN, 7)
        game.submit(CastSpell(you, breaker, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        game.submitTargetSelection(you, listOf(land)).error shouldBe null
        game.bothPass()
        game.getExile(opponent).contains(land) shouldBe true
        game.findPermanent(you, "World Breaker") shouldBe null
        game.bothPass()
        game.findPermanent(you, "World Breaker") shouldNotBe null
    }

    test("graveyard activation selects and sacrifices a land before returning to hand") {
        val game = driver()
        val you = game.activePlayer!!
        val breaker = game.putCardInGraveyard(you, "World Breaker")
        val first = game.putLandOnBattlefield(you, "Forest")
        val second = game.putLandOnBattlefield(you, "Forest")
        game.giveMana(you, Color.GREEN, 2)
        game.giveColorlessMana(you, 1)
        game.submit(ActivateAbility(you, breaker, WorldBreaker.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        game.submitCardSelection(you, listOf(first)).error shouldBe null
        game.getGraveyard(you).contains(first) shouldBe true
        game.getGraveyard(you).contains(second) shouldBe false
        game.getGraveyard(you).contains(breaker) shouldBe true
        game.bothPass()
        game.getHand(you).contains(breaker) shouldBe true
        game.getGraveyard(you).contains(breaker) shouldBe false
        game.getHand(game.getOpponent(you)).contains(breaker) shouldBe false
    }

    test("colored mana cannot pay the colorless requirement") {
        val game = driver()
        val you = game.activePlayer!!
        val breaker = game.putCardInGraveyard(you, "World Breaker")
        val land = game.putLandOnBattlefield(you, "Forest")
        game.giveMana(you, Color.GREEN, 3)
        game.submit(ActivateAbility(you, breaker, WorldBreaker.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        game.getGraveyard(you).contains(breaker) shouldBe true
        game.getGraveyard(you).contains(land) shouldBe false
    }

    test("the return ability cannot be activated from the battlefield") {
        val game = driver()
        val you = game.activePlayer!!
        val breaker = game.putCreatureOnBattlefield(you, "World Breaker")
        game.putLandOnBattlefield(you, "Forest")
        game.giveColorlessMana(you, 3)
        game.submit(ActivateAbility(you, breaker, WorldBreaker.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        game.findPermanent(you, "World Breaker") shouldBe breaker
    }
})
