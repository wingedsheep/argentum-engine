package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.OneLastJob
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * One Last Job — {2}{W} Sorcery, Spree.
 *
 * + {2} — Return target creature card from your graveyard to the battlefield.
 * + {1} — Return target Mount or Vehicle card from your graveyard to the battlefield.
 * + {1} — Return target Aura or Equipment card from your graveyard to the battlefield attached
 *         to a creature you control.
 *
 * Mode 3 exercises the new [Effects.PutOntoBattlefieldAttachedToChosen] effect: the host creature
 * is chosen at resolution (not a target) and the Aura/Equipment enters attached to it.
 */
class OtjOneLastJobScenarioTest : FunSpec({

    // A small Aura that buffs the enchanted creature, for the mode-3 attach path.
    val testAura = card("Test Buff Aura") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nEnchanted creature gets +2/+2."
        auraTarget = Targets.Creature
        staticAbility { ability = ModifyStats(2, 2) }
    }

    // A small Equipment, for the mode-3 attach path.
    val testEquip = card("Test Power Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equipped creature gets +1/+0.\nEquip {1}"
        staticAbility { ability = ModifyStats(1, 0) }
        equipAbility("{1}")
    }

    // A Mount creature card, for mode 2 (Mount/Vehicle).
    val testMount = card("Test Mustang") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Mount"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OneLastJob, testAura, testEquip, testMount))
        return driver
    }

    test("mode + {2}: return target creature card from your graveyard to the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val courser = driver.putCardInGraveyard(me, "Centaur Courser")

        val spell = driver.putCardInHand(me, "One Last Job")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 4) // {2}{W} base + {2} for the mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Card(courser, me, Zone.GRAVEYARD)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Card(courser, me, Zone.GRAVEYARD))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(me, "Centaur Courser") shouldBe courser
        driver.getGraveyard(me).contains(courser) shouldBe false
    }

    test("mode + {1}: return target Mount card from your graveyard to the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val mount = driver.putCardInGraveyard(me, "Test Mustang")

        val spell = driver.putCardInHand(me, "One Last Job")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 3) // {2}{W} base + {1} for the mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Card(mount, me, Zone.GRAVEYARD)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Card(mount, me, Zone.GRAVEYARD))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(me, "Test Mustang") shouldBe mount
    }

    test("mode + {1}: return target Equipment attached to a chosen creature you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val host = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val equip = driver.putCardInGraveyard(me, "Test Power Blade")

        val spell = driver.putCardInHand(me, "One Last Job")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 3) // {2}{W} base + {1} for the mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Card(equip, me, Zone.GRAVEYARD)),
                chosenModes = listOf(2),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Card(equip, me, Zone.GRAVEYARD))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // The mode pauses for the controller to choose the host creature.
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(host))
        driver.isPaused shouldBe false

        // The Equipment is on the battlefield, attached to the chosen creature.
        driver.findPermanent(me, "Test Power Blade") shouldBe equip
        driver.state.getEntity(equip)?.get<AttachedToComponent>()?.targetId shouldBe host
        driver.getGraveyard(me).contains(equip) shouldBe false
    }

    test("mode + {1}: return target Aura attached to a chosen creature you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val host = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val aura = driver.putCardInGraveyard(me, "Test Buff Aura")

        val spell = driver.putCardInHand(me, "One Last Job")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 3)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Card(aura, me, Zone.GRAVEYARD)),
                chosenModes = listOf(2),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Card(aura, me, Zone.GRAVEYARD))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(host))
        driver.isPaused shouldBe false

        // The Aura entered attached to the chosen creature.
        driver.findPermanent(me, "Test Buff Aura") shouldBe aura
        driver.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe host
    }

    test("Spree requires at least one mode: casting with no modes fails") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val spell = driver.putCardInHand(me, "One Last Job")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 2)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = emptyList(),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe false
    }
})
