package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.LifePaymentPurpose
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.PlayersCantPayLifeForCosts
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [PlayersCantPayLifeForCosts] — "Players can't pay life to cast spells or to activate abilities
 * [that aren't mana abilities]" (Karn's Sylex). Each case pins a printed ruling or rule:
 *
 *  - a spell whose cost requires life can't be cast; an activated (non-mana) ability whose cost
 *    requires life can't be activated — offered or submitted (ruling);
 *  - a Phyrexian pip can't be paid with life, but can still be paid with mana (CR 107.4f);
 *  - a mana ability's life cost stays payable under the "aren't mana abilities" form, and doesn't
 *    under the plain form;
 *  - life paid while a spell resolves is unaffected (ruling: "a resolving spell or ability");
 *  - paying 0 life is always allowed (ruling; CR 119.4b);
 *  - it binds every player, and stops when its source loses its abilities.
 */
class PlayersCantPayLifeForCostsTest : FunSpec({

    val sylexLike = card("Life Cost Lock") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "Players can't pay life to cast spells or to activate abilities that aren't mana abilities."
        staticAbility { ability = PlayersCantPayLifeForCosts(excludeManaAbilities = true) }
    }
    val totalLock = card("Total Life Cost Lock") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "Players can't pay life to cast spells or to activate abilities."
        staticAbility { ability = PlayersCantPayLifeForCosts() }
    }
    val lifeSpell = card("Life Cost Sorcery") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        oracleText = "As an additional cost to cast this spell, pay 2 life.\nDraw a card."
        additionalCost(Costs.additional.PayLife(2))
        spell { effect = Effects.DrawCards(1) }
    }
    val xLifeSpell = card("X Life Cost Sorcery") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        oracleText = "As an additional cost to cast this spell, pay X life.\nDraw a card."
        additionalCost(Costs.additional.PayXLife())
        spell { effect = Effects.DrawCards(1) }
    }
    val phyrexianSpell = card("Phyrexian Cost Sorcery") {
        manaCost = "{U/P}"
        typeLine = "Sorcery"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }
    val resolvingPayment = card("Resolving Life Payment") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        oracleText = "Pay 3 life. Draw a card."
        spell { effect = Effects.PayLife(3).then(Effects.DrawCards(1)) }
    }
    val lifeEngine = card("Life Cost Engine") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Pay 1 life: Draw a card."
        activatedAbility {
            cost = Costs.PayLife(1)
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }
    val painSource = card("Life Cost Mana Rock") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{T}, Pay 1 life: Add {U}."
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
            effect = Effects.AddMana(Color.BLUE)
            manaAbility = true
        }
    }
    val mute = card("Life Lock Mute") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Target permanent loses all abilities until end of turn."
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) }
    }

    fun newDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                sylexLike, totalLock, lifeSpell, xLifeSpell, phyrexianSpell, resolvingPayment, lifeEngine, painSource, mute
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to me
    }

    fun GameTestDriver.canCast(player: EntityId, cardId: EntityId) =
        legalActions(player).any { (it.action as? CastSpell)?.cardId == cardId && it.affordable }

    fun GameTestDriver.canActivate(player: EntityId, sourceId: EntityId) =
        legalActions(player).any { (it.action as? ActivateAbility)?.sourceId == sourceId && it.affordable }

    test("a spell with a life additional cost can't be cast, offered or submitted") {
        val (driver, me) = newDriver()
        driver.putLandOnBattlefield(me, "Island")
        val spell = driver.putCardInHand(me, "Life Cost Sorcery")
        withClue("castable without the lock") { driver.canCast(me, spell) shouldBe true }

        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Life Cost Lock")
        withClue("an opponent's lock binds every player") { driver.canCast(me, spell) shouldBe false }
        driver.submit(CastSpell(playerId = me, cardId = spell)).error shouldNotBe null
        driver.getLifeTotal(me) shouldBe 20
    }

    test("pay X life may still be paid with X = 0, never more") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Cost Lock")
        driver.putLandOnBattlefield(me, "Island")
        driver.putLandOnBattlefield(me, "Island")
        val spell = driver.putCardInHand(me, "X Life Cost Sorcery")

        val offer = driver.legalActions(me).first { (it.action as? CastSpell)?.cardId == spell }
        withClue("X is capped at 0") { offer.additionalCostInfo?.payXLifeMaxX shouldBe 0 }
        val payThree = com.wingedsheep.sdk.scripting.AdditionalCostPayment(payXLifeAmount = 3)
        driver.submit(CastSpell(playerId = me, cardId = spell, additionalCostPayment = payThree)).error shouldNotBe null
        driver.getLifeTotal(me) shouldBe 20

        val payZero = com.wingedsheep.sdk.scripting.AdditionalCostPayment(payXLifeAmount = 0)
        driver.submitSuccess(CastSpell(playerId = me, cardId = spell, additionalCostPayment = payZero))
        driver.getLifeTotal(me) shouldBe 20
    }

    test("a Phyrexian pip can't be paid with life, but mana still pays it") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Cost Lock")
        val spell = driver.putCardInHand(me, "Phyrexian Cost Sorcery")

        withClue("no mana: unaffordable, since life is off the table") { driver.canCast(me, spell) shouldBe false }
        val lifePayment = PaymentStrategy.Explicit(manaAbilitiesToActivate = emptyList(), phyrexianLifePayments = listOf(Color.BLUE))
        driver.submit(CastSpell(playerId = me, cardId = spell, paymentStrategy = lifePayment)).error shouldNotBe null
        driver.getLifeTotal(me) shouldBe 20

        val island = driver.putLandOnBattlefield(me, "Island")
        driver.canCast(me, spell) shouldBe true
        driver.submitSuccess(CastSpell(playerId = me, cardId = spell))
        driver.isTapped(island) shouldBe true
        driver.getLifeTotal(me) shouldBe 20
    }

    test("a non-mana activated ability with a life cost can't be activated") {
        val (driver, me) = newDriver()
        val engine = driver.putPermanentOnBattlefield(me, "Life Cost Engine")
        val abilityId = lifeEngine.activatedAbilities.single().id
        withClue("activatable without the lock") { driver.canActivate(me, engine) shouldBe true }

        driver.putPermanentOnBattlefield(me, "Life Cost Lock")
        driver.canActivate(me, engine) shouldBe false
        driver.submit(ActivateAbility(playerId = me, sourceId = engine, abilityId = abilityId)).error shouldNotBe null
        withClue("an explicit payment strategy doesn't slip past the check") {
            driver.submit(
                ActivateAbility(playerId = me, sourceId = engine, abilityId = abilityId, paymentStrategy = PaymentStrategy.Explicit(manaAbilitiesToActivate = emptyList()))
            ).error shouldNotBe null
        }
        driver.getLifeTotal(me) shouldBe 20
    }

    test("a mana ability's life cost stays payable under 'that aren't mana abilities' — even to pay for a spell") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Cost Lock")
        val rock = driver.putPermanentOnBattlefield(me, "Life Cost Mana Rock")
        val abilityId = painSource.activatedAbilities.single().id
        val spell = driver.putCardInHand(me, "Resolving Life Payment")
        withClue("auto-pay may use the pain rock to cast a spell") { driver.canCast(me, spell) shouldBe true }

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = rock, abilityId = abilityId))
        driver.getLifeTotal(me) shouldBe 19
        driver.state.getEntity(me)!!.get<ManaPoolComponent>()!!.blue shouldBe 1
        driver.state.canPayLife(me, 1, LifePaymentPurpose.ACTIVATE_MANA_ABILITY) shouldBe true
        driver.state.canPayLife(me, 1, LifePaymentPurpose.ACTIVATE_ABILITY) shouldBe false
    }

    test("the plain form forbids a mana ability's life cost too") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Total Life Cost Lock")
        val rock = driver.putPermanentOnBattlefield(me, "Life Cost Mana Rock")
        val abilityId = painSource.activatedAbilities.single().id

        driver.submit(ActivateAbility(playerId = me, sourceId = rock, abilityId = abilityId)).error shouldNotBe null
        driver.state.getEntity(rock)!!.has<TappedComponent>() shouldBe false
        driver.getLifeTotal(me) shouldBe 20
        withClue("auto-pay can't lean on it either") {
            val spell = driver.putCardInHand(me, "Phyrexian Cost Sorcery")
            driver.canCast(me, spell) shouldBe false
        }
    }

    test("life paid while a spell resolves is unaffected") {
        val (driver, me) = newDriver()
        driver.putPermanentOnBattlefield(me, "Life Cost Lock")
        driver.putLandOnBattlefield(me, "Island")
        val spell = driver.putCardInHand(me, "Resolving Life Payment")
        val hand = driver.getHandSize(me)

        driver.submitSuccess(CastSpell(playerId = me, cardId = spell))
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe 17
        driver.getHandSize(me) shouldBe hand
        driver.state.canPayLife(me, 3, LifePaymentPurpose.OTHER) shouldBe true
    }

    test("a lock that has lost its abilities forbids nothing; leaving the battlefield ends it") {
        val (driver, me) = newDriver()
        val lock = driver.putPermanentOnBattlefield(me, "Life Cost Lock")
        driver.state.canPayLife(me, 2, LifePaymentPurpose.CAST_SPELL) shouldBe false

        val muteCard = driver.putCardInHand(me, "Life Lock Mute")
        driver.castSpellWithTargets(me, muteCard, listOf(ChosenTarget.Permanent(lock))).error shouldBe null
        driver.bothPass()
        driver.state.canPayLife(me, 2, LifePaymentPurpose.CAST_SPELL) shouldBe true

        val second = driver.putPermanentOnBattlefield(me, "Life Cost Lock")
        driver.state.canPayLife(me, 2, LifePaymentPurpose.CAST_SPELL) shouldBe false
        driver.moveToGraveyard(second)
        driver.state.canPayLife(me, 2, LifePaymentPurpose.CAST_SPELL) shouldBe true
    }
})
