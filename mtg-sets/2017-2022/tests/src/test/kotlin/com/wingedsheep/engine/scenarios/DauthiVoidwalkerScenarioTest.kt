package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.DauthiVoidwalker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Dauthi Voidwalker (MH2 #81):
 *   "If a card would be put into an opponent's graveyard from anywhere, instead exile it with a
 *    void counter on it.
 *    {T}, Sacrifice this creature: Choose an exiled card an opponent owns with a void counter on it.
 *    You may play it this turn without paying its mana cost."
 *
 * "From anywhere" includes the stack, so the void counter has to arrive on a spell that is
 * countered or fizzles as well as one that resolves — the two stack paths that used to drop the
 * replacement's rider.
 */
class DauthiVoidwalkerScenarioTest : FunSpec({

    fun start(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + DauthiVoidwalker)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.voidCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.VOID) ?: 0

    fun GameTestDriver.shouldBeVoided(owner: EntityId, card: EntityId) {
        getGraveyard(owner) shouldNotContain card
        getExile(owner) shouldContain card
        voidCounters(card) shouldBe 1
    }

    test("an opponent's creature that dies is exiled with a void counter instead") {
        val d = start()
        val you = d.player1
        val opp = d.getOpponent(you)
        d.putCreatureOnBattlefield(you, "Dauthi Voidwalker")
        val lions = d.putCreatureOnBattlefield(opp, "Savannah Lions")

        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, listOf(lions)).error shouldBe null
        d.bothPass()

        d.shouldBeVoided(opp, lions)
        // Your own spell goes to your own graveyard untouched.
        d.getGraveyard(you) shouldContain bolt
        d.voidCounters(bolt) shouldBe 0
    }

    test("an opponent's spell that resolves is exiled with a void counter") {
        val d = start()
        val you = d.player1
        val opp = d.getOpponent(you)
        d.putCreatureOnBattlefield(you, "Dauthi Voidwalker")
        val bears = d.putCreatureOnBattlefield(you, "Grizzly Bears")

        // You cast Giant Growth and pass; the opponent answers with a Bolt at your face.
        val growth = d.putCardInHand(you, "Giant Growth")
        d.giveMana(you, Color.GREEN, 1)
        d.castSpell(you, growth, listOf(bears)).error shouldBe null
        d.passPriority(you)
        val oppBolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)
        d.castSpell(opp, oppBolt, listOf(you)).error shouldBe null
        d.bothPass()

        d.shouldBeVoided(opp, oppBolt)
    }

    test("an opponent's countered spell is exiled with a void counter") {
        val d = start()
        val you = d.player1
        val opp = d.getOpponent(you)
        d.putCreatureOnBattlefield(you, "Dauthi Voidwalker")
        val bears = d.putCreatureOnBattlefield(you, "Grizzly Bears")

        val growth = d.putCardInHand(you, "Giant Growth")
        d.giveMana(you, Color.GREEN, 1)
        d.castSpell(you, growth, listOf(bears)).error shouldBe null
        d.passPriority(you)
        val oppBolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)
        d.castSpell(opp, oppBolt, listOf(you)).error shouldBe null
        d.passPriority(opp)

        val counterspell = d.putCardInHand(you, "Counterspell")
        d.giveMana(you, Color.BLUE, 2)
        d.castSpellWithTargets(you, counterspell, listOf(ChosenTarget.Spell(oppBolt))).error shouldBe null
        d.bothPass()

        d.shouldBeVoided(opp, oppBolt)
        d.getGraveyard(you) shouldContain counterspell
    }

    test("an opponent's spell that fizzles is exiled with a void counter") {
        val d = start()
        val you = d.player1
        val opp = d.getOpponent(you)
        d.putCreatureOnBattlefield(you, "Dauthi Voidwalker")
        val bears = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val lions = d.putCreatureOnBattlefield(opp, "Savannah Lions")

        val growth = d.putCardInHand(you, "Giant Growth")
        d.giveMana(you, Color.GREEN, 1)
        d.castSpell(you, growth, listOf(bears)).error shouldBe null
        d.passPriority(you)
        // The opponent pumps their Lions; you kill the Lions in response, so the pump fizzles.
        val oppGrowth = d.putCardInHand(opp, "Giant Growth")
        d.giveMana(opp, Color.GREEN, 1)
        d.castSpell(opp, oppGrowth, listOf(lions)).error shouldBe null
        d.passPriority(opp)
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, listOf(lions)).error shouldBe null
        d.bothPass() // Bolt resolves: the Lions are voided
        d.bothPass() // the opponent's Giant Growth fizzles

        d.shouldBeVoided(opp, lions)
        d.shouldBeVoided(opp, oppGrowth)
    }

    test("sacrificing it lets you play a voided card this turn without paying its mana cost") {
        val d = start()
        val you = d.player1
        val opp = d.getOpponent(you)
        val voidwalker = d.putCreatureOnBattlefield(you, "Dauthi Voidwalker")
        d.removeSummoningSickness(voidwalker)
        val lions = d.putCreatureOnBattlefield(opp, "Savannah Lions")
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, listOf(lions)).error shouldBe null
        d.bothPass()
        d.shouldBeVoided(opp, lions)

        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = voidwalker,
                abilityId = DauthiVoidwalker.activatedAbilities.single().id
            )
        ).error shouldBe null
        d.bothPass()
        // The Lions are the only eligible card, so the choice makes itself.
        (d.pendingDecision as? SelectCardsDecision)?.let { decision ->
            decision.options shouldBe listOf(lions)
            d.submitCardSelection(you, listOf(lions))
        }

        // No mana in pool and no lands: the {W} creature can only be cast for free.
        d.castSpell(you, lions).error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Savannah Lions") shouldBe lions
        d.getGraveyard(you) shouldContain voidwalker
    }
})
