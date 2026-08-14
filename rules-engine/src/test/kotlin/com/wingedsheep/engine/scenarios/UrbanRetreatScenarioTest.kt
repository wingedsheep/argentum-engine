package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.UrbanRetreat
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Urban Retreat (SPM #187) — Land.
 *
 *  - "This land enters tapped." — [EntersTapped] replacement; enters tapped no matter how it
 *    reaches the battlefield.
 *  - "{T}: Add {G}, {W}, or {U}." — mana ability over
 *    [ManaColorSet.Specific][com.wingedsheep.sdk.scripting.values.ManaColorSet.Specific] of exactly
 *    green/white/blue.
 *  - "{2}, Return a tapped creature you control to its owner's hand: Put this card from your hand
 *    onto the battlefield. Activate only as a sorcery." — a from-hand activated ability
 *    (activateFromZone = HAND) whose additional cost bounces a tapped creature you control
 *    ([Costs.ReturnToHand] over `Creature.tapped()`) and whose effect puts the source (this land, in
 *    hand) onto the battlefield.
 */
class UrbanRetreatScenarioTest : FunSpec({

    val manaAbilityId = UrbanRetreat.activatedAbilities.first { it.isManaAbility }.id
    val fromHandAbilityId = UrbanRetreat.activatedAbilities.first { !it.isManaAbility }.id

    fun pool(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId): ManaPoolComponent =
        driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
        return driver
    }

    test("enters tapped when played from hand") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val landInHand = driver.putCardInHand(p1, "Urban Retreat")
        driver.playLand(p1, landInHand).isSuccess shouldBe true

        val permanent = driver.findPermanent(p1, "Urban Retreat")
        permanent.shouldNotBeNull()
        driver.isTapped(permanent) shouldBe true
    }

    test("the mana ability offers exactly {G}, {W}, or {U}") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val land = driver.putLandOnBattlefield(p1, "Urban Retreat")
        driver.untapPermanent(land)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val result = driver.submit(ActivateAbility(playerId = p1, sourceId = land, abilityId = manaAbilityId))
        result.error shouldBe null

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        decision.availableColors.shouldContainExactly(Color.GREEN, Color.WHITE, Color.BLUE)
    }

    test("taps for green when green is chosen") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val land = driver.putLandOnBattlefield(p1, "Urban Retreat")
        driver.untapPermanent(land)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // manaColorChoice supplies the color up front, so the ability resolves without a pause.
        driver.submitSuccess(
            ActivateAbility(playerId = p1, sourceId = land, abilityId = manaAbilityId, manaColorChoice = Color.GREEN)
        )

        driver.isTapped(land) shouldBe true
        val manaPool = pool(driver, p1)
        manaPool.green shouldBe 1
        manaPool.white shouldBe 0
        manaPool.blue shouldBe 0
    }

    test("taps for white or blue when chosen") {
        for (color in listOf(Color.WHITE, Color.BLUE)) {
            val driver = setup()
            val p1 = driver.activePlayer!!
            val land = driver.putLandOnBattlefield(p1, "Urban Retreat")
            driver.untapPermanent(land)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            driver.submitSuccess(
                ActivateAbility(playerId = p1, sourceId = land, abilityId = manaAbilityId, manaColorChoice = color)
            )

            val manaPool = pool(driver, p1)
            when (color) {
                Color.WHITE -> manaPool.white shouldBe 1
                Color.BLUE -> manaPool.blue shouldBe 1
                else -> error("unreachable")
            }
        }
    }

    test("from-hand ability bounces a tapped creature and puts Urban Retreat onto the battlefield tapped") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val landInHand = driver.putCardInHand(p1, "Urban Retreat")
        val creature = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.tapPermanent(creature)
        driver.giveColorlessMana(p1, 2)

        // {2}, Return the tapped Savannah Lions you control: put Urban Retreat from hand onto the battlefield.
        driver.submitSuccess(
            ActivateAbility(
                playerId = p1,
                sourceId = landInHand,
                abilityId = fromHandAbilityId,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(creature)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        // The activated ability goes on the stack; resolve it.
        driver.bothPass()

        // The tapped creature returned to its owner's hand.
        driver.getHand(p1).contains(creature) shouldBe true
        driver.findPermanent(p1, "Savannah Lions") shouldBe null

        // Urban Retreat left hand and is now a land permanent, entering tapped via its replacement.
        driver.getHand(p1).contains(landInHand) shouldBe false
        val landPermanent = driver.findPermanent(p1, "Urban Retreat")
        landPermanent.shouldNotBeNull()
        driver.isTapped(landPermanent) shouldBe true
    }

    test("from-hand ability cannot be activated without a tapped creature to return") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val landInHand = driver.putCardInHand(p1, "Urban Retreat")
        // An untapped creature does not satisfy the "return a tapped creature" cost.
        val creature = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.untapPermanent(creature)
        driver.giveColorlessMana(p1, 2)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = landInHand,
                abilityId = fromHandAbilityId,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(creature)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        (result.error != null) shouldBe true
        driver.findPermanent(p1, "Urban Retreat") shouldBe null
    }
})
