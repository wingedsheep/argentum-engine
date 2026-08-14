package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.ForgeAnew
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Forge Anew — ETB reanimates a target Equipment from your graveyard, then grants instant-speed
 * equip during your turn and a free first equip each turn (the equipment-aware permissions).
 */
class ForgeAnewScenarioTest : FunSpec({

    val equipId = Bonesplitter.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ForgeAnew + Bonesplitter)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("ETB returns a target Equipment card from your graveyard to the battlefield") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sword = driver.putCardInGraveyard(you, "Bonesplitter")
        val forge = driver.putCardInHand(you, "Forge Anew")
        driver.giveMana(you, Color.WHITE, 1)
        driver.giveColorlessMana(you, 2)

        driver.castSpell(you, forge)
        driver.bothPass() // resolve the enchantment; its ETB trigger then asks for a target
        driver.submitTargetSelection(you, listOf(sword))
        driver.bothPass() // resolve the ETB trigger

        // Bonesplitter is back on the battlefield.
        driver.state.getBattlefield(you).contains(sword) shouldBe true
    }

    test("grants a free first equip each turn, then the second equip costs full") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val a = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val b = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val sword = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        driver.putPermanentOnBattlefield(you, "Forge Anew")

        // First equip this turn is free even with no mana.
        driver.submit(
            ActivateAbility(you, sword, equipId, targets = listOf(ChosenTarget.Permanent(a)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe a

        // Second equip this turn must pay {1}; with no mana it fails.
        driver.submitExpectFailure(
            ActivateAbility(you, sword, equipId, targets = listOf(ChosenTarget.Permanent(b)))
        )
        driver.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe a
    }

    test("the equip ability is ENUMERATED at begin-of-combat and after declaring attackers") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val sword = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        driver.putPermanentOnBattlefield(you, "Forge Anew")
        driver.removeSummoningSickness(courser)
        driver.giveColorlessMana(you, 1)

        val services = com.wingedsheep.engine.core.EngineServices(driver.cardRegistry)
        val enumerator = com.wingedsheep.engine.legalactions.LegalActionEnumerator(
            services.cardRegistry, services.manaSolver, services.costCalculator,
            services.predicateEvaluator, services.conditionEvaluator, services.turnManager
        )
        fun equipOffered(): Boolean {
            val pid = driver.state.priorityPlayerId ?: return false
            return enumerator.enumerate(driver.state, pid).any {
                val a = it.action
                a is ActivateAbility && a.sourceId == sword
            }
        }

        // At begin-of-combat (a normal priority window), the equip is offered.
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        equipOffered() shouldBe true

        // After attackers are declared, the declare-attackers priority window also offers it.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(courser), opponent)
        equipOffered() shouldBe true
    }

    test("a free first equip is listed as {0}, not the Equipment's printed mana cost") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val sword = driver.putPermanentOnBattlefield(you, "Loxodon Warhammer") // printed/equip cost {3}
        driver.putPermanentOnBattlefield(you, "Forge Anew")

        val services = com.wingedsheep.engine.core.EngineServices(driver.cardRegistry)
        val enumerator = com.wingedsheep.engine.legalactions.LegalActionEnumerator(
            services.cardRegistry, services.manaSolver, services.costCalculator,
            services.predicateEvaluator, services.conditionEvaluator, services.turnManager
        )
        val pid = driver.state.priorityPlayerId!!
        val equip = enumerator.enumerate(driver.state, pid).first {
            val a = it.action
            a is ActivateAbility && a.sourceId == sword
        }
        // The first equip each turn is free — it must read as {0}, not blank (which the client
        // would render as the Equipment's printed {3} via `manaCostString || cardInfo.manaCost`).
        equip.manaCostString shouldBe "{0}"
        // An equip ability renders as its printed keyword line (CR 702.6a), and because that line is
        // derived rather than a frozen descriptionOverride it picks up the discount: "Equip {0}",
        // not the printed "Equip {3}".
        equip.description shouldBe "Equip {0}"
    }

    test("grants instant-speed equip during your turn (equip in combat)") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val sword = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        driver.putPermanentOnBattlefield(you, "Forge Anew")
        driver.giveColorlessMana(you, 1) // ensure it's timing, not the free discount, being tested

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.submit(
            ActivateAbility(you, sword, equipId, targets = listOf(ChosenTarget.Permanent(courser)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe courser
    }
})
