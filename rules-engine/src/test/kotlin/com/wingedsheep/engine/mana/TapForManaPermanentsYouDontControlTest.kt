package com.wingedsheep.engine.mana

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.TapForManaGrantsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `TapForManaPermanentsYouDontControlEffect` — "until end of turn, you may tap lands you don't
 * control for mana. Spend this mana only to cast spells." (Piracy).
 *
 * CR 602.2: only a permanent's controller may activate its abilities unless something says
 * otherwise; the grant is that something, for {T} mana abilities only (CR 106.12). The activator
 * controls the ability (CR 113.8), so the mana lands in their pool, and it carries the grant's
 * restriction: spells yes, ability costs no. Auto-pay may borrow the lands for a spell, never for
 * an ability. The grant ends at cleanup.
 */
class TapForManaPermanentsYouDontControlTest : FunSpec({

    val piracy = card("Test Piracy") {
        manaCost = "{U}{U}"
        typeLine = "Sorcery"
        oracleText = "Until end of turn, you may tap lands you don't control for mana. Spend this mana only to cast spells."
        spell {
            effect = Effects.TapForManaPermanentsYouDontControl(
                permanentFilter = GameObjectFilter.Land,
                restriction = ManaRestriction.SpellsOnly,
            )
        }
    }

    val pumpable = card("Test Pumpable Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        oracleText = "{1}: This creature gets +1/+0 until end of turn."
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
            timing = TimingRule.InstantSpeed
        }
    }

    /** Player 1 has resolved Test Piracy; player 2 controls two Forests and a Llanowar Elves. */
    fun afterPiracy(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(piracy, pumpable))
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true, startingPlayer = 0)
        val me = driver.player1
        val opponent = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putLandOnBattlefield(opponent, "Forest")
        driver.putLandOnBattlefield(opponent, "Forest")
        driver.putCreatureOnBattlefield(opponent, "Llanowar Elves").also { driver.removeSummoningSickness(it) }

        driver.giveMana(me, Color.BLUE, 2)
        driver.castSpell(me, driver.putCardInHand(me, "Test Piracy")).error shouldBe null
        driver.bothPass()
        return Triple(driver, me, opponent)
    }

    fun manaActionsOn(driver: GameTestDriver, player: EntityId, sourceId: EntityId) =
        driver.legalActions(player).filter { it.isManaAbility && (it.action as? ActivateAbility)?.sourceId == sourceId }

    fun pool(driver: GameTestDriver, player: EntityId) =
        driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("lands you don't control are offered for mana; their creatures aren't") {
        val (driver, me, opponent) = afterPiracy()
        val forest = driver.getLands(opponent).first()
        val elves = driver.getCreatures(opponent).first()

        manaActionsOn(driver, me, forest).size shouldBe 1
        manaActionsOn(driver, me, elves).shouldBeEmpty()
        // The opponent gains nothing from my grant.
        manaActionsOn(driver, opponent, driver.putLandOnBattlefield(me, "Island")).shouldBeEmpty()
    }

    test("tapping an opponent's land puts spells-only mana in my pool") {
        val (driver, me, opponent) = afterPiracy()
        val forest = driver.getLands(opponent).first()

        driver.submit(manaActionsOn(driver, me, forest).single().action).error shouldBe null

        driver.isTapped(forest) shouldBe true
        val restricted = pool(driver, me).restrictedMana
        restricted.size shouldBe 1
        restricted.single().color shouldBe Color.GREEN
        restricted.single().restriction shouldBe ManaRestriction.SpellsOnly
        pool(driver, opponent).total shouldBe 0
    }

    test("auto-pay borrows an opponent's lands to cast a spell") {
        val (driver, me, opponent) = afterPiracy()

        driver.castSpell(me, driver.putCardInHand(me, "Grizzly Bears")).error shouldBe null

        driver.getLands(opponent).forEach { driver.isTapped(it) shouldBe true }
        driver.bothPass()
        driver.getCreatures(me).map { driver.getCardName(it) } shouldContain "Grizzly Bears"
    }

    test("borrowed mana can't pay an ability cost, by auto-pay or from the pool") {
        val (driver, me, opponent) = afterPiracy()
        val bear = driver.putCreatureOnBattlefield(me, "Test Pumpable Bear")
        val pump = driver.legalActions(me)
            .filter { (it.action as? ActivateAbility)?.sourceId == bear }
        // No land of my own: the only mana in reach is borrowed, and it's spells-only.
        pump.single().affordable shouldBe false

        val forest = driver.getLands(opponent).first()
        driver.submit(manaActionsOn(driver, me, forest).single().action).error shouldBe null
        val abilityId = driver.cardRegistry.getCard("Test Pumpable Bear")!!.script.activatedAbilities.single().id
        driver.submit(ActivateAbility(me, bear, abilityId, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        driver.submit(ActivateAbility(me, bear, abilityId, paymentStrategy = PaymentStrategy.AutoPay)).error shouldNotBe null
        driver.getLands(opponent).count { driver.isTapped(it) } shouldBe 1
    }

    test("my own lands keep making unrestricted mana") {
        val (driver, me, _) = afterPiracy()
        val island = driver.putLandOnBattlefield(me, "Island")

        driver.submit(manaActionsOn(driver, me, island).single().action).error shouldBe null

        pool(driver, me).blue shouldBe 1
        pool(driver, me).restrictedMana.shouldBeEmpty()
    }

    test("the grant ends at cleanup") {
        val (driver, me, opponent) = afterPiracy()
        driver.state.getEntity(me)?.get<TapForManaGrantsComponent>() shouldNotBe null

        driver.passPriorityUntil(Step.UPKEEP)

        driver.state.getEntity(me)?.get<TapForManaGrantsComponent>() shouldBe null
        manaActionsOn(driver, me, driver.getLands(opponent).first()).shouldBeEmpty()
    }
})
