package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.KarnsSylex
import com.wingedsheep.mtg.sets.definitions.nph.cards.GitaxianProbe
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Karn's Sylex (DMU #234) — enters tapped; "Players can't pay life to cast spells or to activate
 * abilities that aren't mana abilities"; "{X}, {T}, Exile Karn's Sylex: Destroy each nonland
 * permanent with mana value X or less. Activate only as a sorcery."
 *
 * The prohibition's edge cases (mana abilities, resolution-time payments, paying 0, ability loss)
 * are pinned in the engine's `PlayersCantPayLifeForCostsTest`; this covers the card as printed.
 */
class KarnsSylexScenarioTest : FunSpec({

    val sylexAbility = KarnsSylex.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + KarnsSylex + GitaxianProbe)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("enters tapped") {
        val d = driver()
        val sylex = d.putCardInHand(d.player1, "Karn's Sylex")
        d.giveColorlessMana(d.player1, 3)
        d.castSpell(d.player1, sylex).error shouldBe null
        d.bothPass()
        d.findPermanent(d.player1, "Karn's Sylex") shouldNotBe null
        d.isTapped(d.findPermanent(d.player1, "Karn's Sylex")!!) shouldBe true
    }

    test("no player can pay Phyrexian mana with life, but mana still casts the spell") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, "Karn's Sylex")
        val probe = d.putCardInHand(d.player1, "Gitaxian Probe")

        val target = listOf(ChosenTarget.Player(d.player2))
        val lifeOnly = PaymentStrategy.Explicit(manaAbilitiesToActivate = emptyList(), phyrexianLifePayments = listOf(Color.BLUE))
        d.submit(CastSpell(playerId = d.player1, cardId = probe, targets = target, paymentStrategy = lifeOnly)).error shouldNotBe null
        d.getLifeTotal(d.player1) shouldBe 20

        d.putLandOnBattlefield(d.player1, "Island")
        d.submit(CastSpell(playerId = d.player1, cardId = probe, targets = target)).error shouldBe null
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("X = 2 destroys each nonland permanent with mana value 2 or less, then the Sylex is gone") {
        val d = driver()
        val sylex = d.putPermanentOnBattlefield(d.player1, "Karn's Sylex")
        val bears = d.putCreatureOnBattlefield(d.player2, "Forest Walker")      // MV 2
        val ownLions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")  // MV 1 — symmetric
        val ogre = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")     // MV 3
        val land = d.putLandOnBattlefield(d.player2, "Island")
        d.giveColorlessMana(d.player1, 2)

        d.submit(ActivateAbility(playerId = d.player1, sourceId = sylex, abilityId = sylexAbility, xValue = 2)).error shouldBe null
        withClue("the Sylex is exiled as a cost") { d.getExile(d.player1).contains(sylex) shouldBe true }
        d.bothPass()

        d.getGraveyard(d.player2).contains(bears) shouldBe true
        d.getGraveyard(d.player1).contains(ownLions) shouldBe true
        d.state.getZone(d.player2, Zone.BATTLEFIELD).contains(ogre) shouldBe true
        withClue("lands survive at any X") { d.state.getZone(d.player2, Zone.BATTLEFIELD).contains(land) shouldBe true }
    }

    test("activate only as a sorcery") {
        val d = driver()
        val sylex = d.putPermanentOnBattlefield(d.player1, "Karn's Sylex")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.giveColorlessMana(d.player1, 1)
        d.submit(ActivateAbility(playerId = d.player1, sourceId = sylex, abilityId = sylexAbility, xValue = 1)).error shouldNotBe null
        d.state.getZone(d.player1, Zone.BATTLEFIELD).contains(sylex) shouldBe true
    }
})
