package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c20.cards.TwinningStaff
import com.wingedsheep.mtg.sets.definitions.khm.cards.ReflectionsOfLittjara
import com.wingedsheep.mtg.sets.definitions.lrw.cards.EyeblightsEnding
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Reflections of Littjara {4}{U} — Enchantment (KHM #73).
 * "As this enchantment enters, choose a creature type.
 *  Whenever you cast a spell of the chosen type, copy that spell."
 *
 * There is no "you may choose new targets" clause, so a copy keeps the original's targets
 * (CR 707.10) — the copy of a Kindred instant is never offered a retarget prompt.
 */
class ReflectionsOfLittjaraScenarioTest : FunSpec({

    val elf = CardDefinition.creature(
        name = "Test Elf",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Elf")),
        power = 1,
        toughness = 1
    )

    /** Reflections of Littjara on your battlefield with Elf chosen; returns the driver, you, the opponent. */
    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ReflectionsOfLittjara, EyeblightsEnding, TwinningStaff, elf))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val reflections = driver.putCardInHand(you, "Reflections of Littjara")
        driver.giveMana(you, Color.BLUE, 5)
        driver.submitSuccess(CastSpell(you, reflections, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()
        val decision = driver.pendingDecision as ChooseOptionDecision
        val index = decision.options.indexOfFirst { it.equals("Elf", ignoreCase = true) }
        driver.submitDecision(you, OptionChosenResponse(decision.id, index))
        return Triple(driver, you, opponent)
    }

    fun spellCopies(driver: GameTestDriver) = driver.state.stack.filter { id ->
        val c = driver.state.getEntity(id)
        c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
    }

    test("a copied Kindred instant keeps the original's target, with no retarget prompt") {
        val (driver, you, opponent) = setup()
        val bearA = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val bearB = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.giveMana(you, Color.BLACK, 3)
        val ending = driver.putCardInHand(you, "Eyeblight's Ending")
        driver.castSpellWithTargets(you, ending, listOf(ChosenTarget.Permanent(bearA))).error shouldBe null

        // Resolve the copy trigger: another legal target (bearB) exists, yet nothing is asked.
        driver.bothPass()
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe false
        val copy = spellCopies(driver).single()
        driver.state.getEntity(copy)!!.get<TargetsComponent>()!!.targets shouldBe
            listOf(ChosenTarget.Permanent(bearA))

        // The copy destroys bearA; the original then fizzles. bearB is untouched.
        driver.bothPass()
        driver.bothPass()
        driver.state.getBattlefield().contains(bearA) shouldBe false
        driver.state.getBattlefield().contains(bearB) shouldBe true
        driver.state.stack.isEmpty() shouldBe true
        driver.state.getZone(you, com.wingedsheep.sdk.core.Zone.GRAVEYARD).contains(ending) shouldBe true
    }

    test("with Twinning Staff, only the additional copy may be retargeted") {
        val (driver, you, opponent) = setup()
        driver.putPermanentOnBattlefield(you, "Twinning Staff")
        val bearA = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val bearB = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.giveMana(you, Color.BLACK, 3)
        val ending = driver.putCardInHand(you, "Eyeblight's Ending")
        driver.castSpellWithTargets(you, ending, listOf(ChosenTarget.Permanent(bearA))).error shouldBe null

        // The trigger's own copy keeps bearA; Twinning Staff's additional copy asks once.
        driver.bothPass()
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(you, listOf(bearB)).error shouldBe null

        spellCopies(driver).map { driver.state.getEntity(it)!!.get<TargetsComponent>()!!.targets }
            .toSet() shouldBe setOf(listOf(ChosenTarget.Permanent(bearA)), listOf(ChosenTarget.Permanent(bearB)))
    }

    test("a creature spell of the chosen type is copied into a token") {
        val (driver, you, _) = setup()

        driver.giveColorlessMana(you, 1)
        val card = driver.putCardInHand(you, "Test Elf")
        driver.submitSuccess(CastSpell(you, card, paymentStrategy = PaymentStrategy.FromPool))
        repeat(3) { if (driver.state.stack.isNotEmpty()) driver.bothPass() }

        driver.state.getBattlefield().count { id ->
            driver.state.getEntity(id)?.get<CardComponent>()?.name == "Test Elf"
        } shouldBe 2
    }

    test("a spell of another type is not copied") {
        val (driver, you, _) = setup()

        driver.giveMana(you, Color.GREEN, 2)
        val bears = driver.putCardInHand(you, "Grizzly Bears")
        driver.submitSuccess(CastSpell(you, bears, paymentStrategy = PaymentStrategy.FromPool))
        driver.state.stack.size shouldBe 1
    }
})
