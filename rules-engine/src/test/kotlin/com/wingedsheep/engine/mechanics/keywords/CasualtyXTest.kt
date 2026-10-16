package com.wingedsheep.engine.mechanics.keywords

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Casualty X (CR 702.153a, Ob Nixilis, the Adversary's rulings): as the spell is cast its controller
 * chooses X and may sacrifice a creature with power X or greater; when they do, the spell is copied.
 * X is the spell's X (CR 601.2b), so the copy shares it (CR 707.10), and the printed copy riders
 * apply — "The copy isn't legendary and has starting loyalty X." — to the token the copy becomes as
 * it resolves (CR 707.10f).
 */
class CasualtyXTest : FunSpec({

    val walker = card("Casualty Walker") {
        manaCost = "{1}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = "Casualty X. The copy isn't legendary and has starting loyalty X."
        keywordAbility(
            KeywordAbility.casualtyX(
                copyExceptions = CopyExceptions(removedSupertypes = setOf(Supertype.LEGENDARY)),
                copyStartingLoyalty = DynamicAmount.XValue
            )
        )
    }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(walker))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        driver.putLandOnBattlefield(caster, "Swamp")
        return driver to caster
    }

    fun resolveAll(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 20) {
            driver.bothPass()
            guard++
        }
    }

    fun walkers(driver: GameTestDriver): List<EntityId> = driver.state.getBattlefield().filter {
        driver.state.getEntity(it)?.get<CardComponent>()?.name == "Casualty Walker"
    }

    fun loyalty(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    fun cast(driver: GameTestDriver, caster: EntityId, spell: EntityId, sacrifice: EntityId?, x: Int? = null) =
        driver.submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                paymentStrategy = PaymentStrategy.AutoPay,
                casualtyCreature = sacrifice,
                xValue = x
            )
        )

    test("the copy is a nonlegendary token whose starting loyalty is the sacrificed creature's power") {
        val (driver, caster) = setup()
        val courser = driver.putCreatureOnBattlefield(caster, "Centaur Courser") // 3/3
        driver.putCreatureOnBattlefield(caster, "Grizzly Bears") // 2/2
        val spell = driver.putCardInHand(caster, "Casualty Walker")

        cast(driver, caster, spell, courser).outcome shouldBe Outcome.Done
        withClue("the casualty creature is sacrificed as a cost") {
            (courser in driver.state.getZone(ZoneKey(caster, Zone.GRAVEYARD))) shouldBe true
        }
        driver.state.getEntity(spell)?.get<SpellOnStackComponent>()?.xValue shouldBe 3

        resolveAll(driver)

        val onBattlefield = walkers(driver)
        withClue("both the original and the copy stay — the copy isn't legendary, so no legend rule") {
            onBattlefield.size shouldBe 2
        }
        val copy = onBattlefield.single { driver.state.getEntity(it)?.has<TokenComponent>() == true }
        val original = onBattlefield.single { it != copy }
        driver.state.getEntity(copy)!!.get<CardComponent>()!!.typeLine.isLegendary shouldBe false
        driver.state.getEntity(original)!!.get<CardComponent>()!!.typeLine.isLegendary shouldBe true
        loyalty(driver, copy) shouldBe 3
        loyalty(driver, original) shouldBe 3
    }

    test("a smaller announced X gives the copy that starting loyalty") {
        val (driver, caster) = setup()
        val courser = driver.putCreatureOnBattlefield(caster, "Centaur Courser") // 3/3
        val spell = driver.putCardInHand(caster, "Casualty Walker")

        cast(driver, caster, spell, courser, x = 1).outcome shouldBe Outcome.Done
        resolveAll(driver)

        val copy = walkers(driver).single { driver.state.getEntity(it)?.has<TokenComponent>() == true }
        loyalty(driver, copy) shouldBe 1
    }

    test("an announced X above the sacrificed creature's power is rejected") {
        val (driver, caster) = setup()
        val bears = driver.putCreatureOnBattlefield(caster, "Grizzly Bears") // 2/2
        val spell = driver.putCardInHand(caster, "Casualty Walker")

        cast(driver, caster, spell, bears, x = 3).outcome shouldNotBe Outcome.Done
        (bears in driver.state.getBattlefield()) shouldBe true
    }

    test("X = 0 sacrificing a 0-power creature: the copy enters with no loyalty and dies") {
        val (driver, caster) = setup()
        val thopter = driver.putCreatureOnBattlefield(caster, "Ornithopter") // 0/2
        val spell = driver.putCardInHand(caster, "Casualty Walker")

        cast(driver, caster, spell, thopter).outcome shouldBe Outcome.Done
        resolveAll(driver)

        val remaining = walkers(driver)
        withClue("the 0-loyalty copy is put into the graveyard by state-based actions") {
            remaining.size shouldBe 1
        }
        driver.state.getEntity(remaining.single())!!.has<TokenComponent>() shouldBe false
        loyalty(driver, remaining.single()) shouldBe 3
    }

    test("declining casualty makes no copy") {
        val (driver, caster) = setup()
        val bears = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        val spell = driver.putCardInHand(caster, "Casualty Walker")

        cast(driver, caster, spell, sacrifice = null).outcome shouldBe Outcome.Done
        resolveAll(driver)

        (bears in driver.state.getBattlefield()) shouldBe true
        walkers(driver).size shouldBe 1
    }

    test("the casualty X cast is offered with X capped at the largest eligible power") {
        val (driver, caster) = setup()
        val bears = driver.putCreatureOnBattlefield(caster, "Grizzly Bears") // 2/2
        val courser = driver.putCreatureOnBattlefield(caster, "Centaur Courser") // 3/3
        val thopter = driver.putCreatureOnBattlefield(caster, "Ornithopter") // 0/2
        driver.putCardInHand(caster, "Casualty Walker")

        val action = driver.legalActions(caster).single { it.actionType == "CastWithCasualty" }
        action.description shouldBe "Cast Casualty Walker (Casualty X)"
        action.hasXCost shouldBe true
        action.maxAffordableX shouldBe 3
        val costInfo = action.additionalCostInfo!!
        costInfo.sacrificeMinPowerIsX shouldBe true
        costInfo.validSacrificeTargets shouldContainExactlyInAnyOrder listOf(bears, courser, thopter)
    }
})
