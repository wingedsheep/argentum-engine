package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.StrongBack
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Strong Back (PIP).
 *
 * - "Equip abilities you activate that target enchanted creature cost {3} less to activate."
 *   ([com.wingedsheep.sdk.scripting.ReduceEquipCost] with a `targetFilter` read relative to Strong Back.)
 * - "Aura spells you cast that target enchanted creature cost {3} less to cast."
 *   (a `YouCast` Aura modifier with a source-relative `FixedIfAnyTargetMatches`.)
 * - "Enchanted creature gets +2/+2 for each Aura and Equipment attached to it."
 *
 * Inline Equip {3} equipment and a {3}{G} Aura make each {3} discount visible.
 */
class StrongBackScenarioTest : FunSpec({

    val testBlade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equip {3}"
        equipAbility("{3}")
    }
    val equipId = testBlade.activatedAbilities.first().id

    val testAura = card("Test Charm") {
        manaCost = "{3}{G}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }

    val testHex = card("Test Hex") {
        manaCost = "{3}{G}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature an opponent controls"
        auraTarget = TargetObject(filter = TargetFilter.CreatureOpponentControls)
    }

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(StrongBack, testBlade, testAura, testHex))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    // Strong Back enchanting [host], without spending mana on casting it.
    fun GameTestDriver.enchant(player: EntityId, host: EntityId): EntityId {
        val back = putPermanentOnBattlefield(player, "Strong Back")
        addComponent(back, AttachedToComponent(host))
        addComponent(host, AttachmentsComponent(listOf(back)))
        return back
    }

    test("enchanted creature gets +2/+2 for each Aura and Equipment attached to it") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.enchant(me, bear)

        driver.state.projectedState.getPower(bear) shouldBe 4
        driver.state.projectedState.getToughness(bear) shouldBe 4

        // Equipping it costs {0} after the {3} discount, and adds a second attachment.
        val blade = driver.putPermanentOnBattlefield(me, "Test Blade")
        driver.submit(
            ActivateAbility(me, blade, equipId, targets = listOf(ChosenTarget.Permanent(bear)))
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe bear
        driver.state.projectedState.getPower(bear) shouldBe 6
        driver.state.projectedState.getToughness(bear) shouldBe 6
    }

    test("equip targeting another creature pays the full cost") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.enchant(me, bear)
        val blade = driver.putPermanentOnBattlefield(me, "Test Blade")

        driver.giveColorlessMana(me, 2)
        driver.submit(
            ActivateAbility(me, blade, equipId, targets = listOf(ChosenTarget.Permanent(other)))
        ).outcome shouldNotBe Outcome.Done
    }

    test("an Aura spell targeting enchanted creature costs {3} less, and is offered as affordable") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.enchant(me, bear)
        driver.putLandOnBattlefield(me, "Forest")
        val charm = driver.putCardInHand(me, "Test Charm")

        // One Forest pays {3}{G} − {3}: enumeration offers the discount before a target is chosen.
        val cast = driver.legalActions(me)
            .firstOrNull { (it.action as? CastSpell)?.cardId == charm }
            .shouldNotBeNull()
        cast.affordable shouldBe true

        // The lone Forest pays the {G} that is still owed.
        driver.castSpell(me, charm, listOf(bear)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getEntity(charm)?.get<AttachedToComponent>()?.targetId shouldBe bear
        driver.state.projectedState.getPower(bear) shouldBe 6
    }

    test("an Aura spell targeting a creature Strong Back doesn't enchant is not discounted") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.enchant(me, bear)
        val charm = driver.putCardInHand(me, "Test Charm")

        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, charm, listOf(other)).outcome shouldNotBe Outcome.Done
    }

    test("an Aura that can't enchant the enchanted creature isn't offered at the discount") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.enchant(me, bear)
        driver.putLandOnBattlefield(me, "Forest")
        val hex = driver.putCardInHand(me, "Test Hex")

        val cast = driver.legalActions(me).firstOrNull { (it.action as? CastSpell)?.cardId == hex }
        (cast == null || !cast.affordable) shouldBe true
    }

    test("an unattached Strong Back offers no discount") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putPermanentOnBattlefield(me, "Strong Back")
        driver.putLandOnBattlefield(me, "Forest")
        val charm = driver.putCardInHand(me, "Test Charm")

        val cast = driver.legalActions(me).firstOrNull { (it.action as? CastSpell)?.cardId == charm }
        (cast == null || !cast.affordable) shouldBe true
    }

    test("the opponent's Aura spells aren't discounted by your Strong Back") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.enchant(me, bear)
        val charm = driver.putCardInHand(opp, "Test Charm")

        // Hand the turn to the opponent so a sorcery-speed Aura is castable at all.
        var safety = 0
        do {
            driver.bothPass()
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        } while (driver.activePlayer != opp && ++safety < 50)

        driver.giveMana(opp, Color.GREEN, 1)
        driver.castSpell(opp, charm, listOf(bear)).outcome shouldNotBe Outcome.Done
        driver.giveColorlessMana(opp, 3)
        driver.castSpell(opp, charm, listOf(bear)).outcome shouldBe Outcome.Done
    }
})
