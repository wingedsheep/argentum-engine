package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 205.1a: when an effect sets an object's card types, the subtypes correlated with a card type it
 * lost are removed too — unless they are also subtypes of a card type it still has. Subtypes of the
 * types it keeps are untouched, and supertypes are never affected (CR 205.4b).
 */
class CardTypeLossSubtypePruningTest : FunSpec({

    fun typeSetter(name: String, vararg types: String) = card(name) {
        manaCost = "{U}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Permanent)
        staticAbility { ability = TransformPermanent(setCardTypes = types.toSet()) }
    }

    val toEnchantment = typeSetter("Prune Probe Enchantment", "ENCHANTMENT")
    val toLand = typeSetter("Prune Probe Land", "LAND")
    val toKindredEnchantment = typeSetter("Prune Probe Kindred Enchantment", "KINDRED", "ENCHANTMENT")

    val shrineSpirit = card("Prune Probe Shrine Spirit") {
        manaCost = "{1}"
        typeLine = "Legendary Enchantment Creature — Shrine Spirit"
        power = 2
        toughness = 2
    }
    val equipmentGolem = card("Prune Probe Equipment Golem") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Equipment Golem"
        power = 2
        toughness = 2
    }
    val forestDryad = card("Prune Probe Forest Dryad") {
        typeLine = "Land Creature — Forest Dryad"
        power = 1
        toughness = 1
    }
    val customType = card("Prune Probe Custom Subtype") {
        manaCost = "{1}"
        typeLine = "Creature — Gloopwhatsit"
        power = 1
        toughness = 1
    }

    fun enchant(permanentName: String, auraName: String): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(toEnchantment, toLand, toKindredEnchantment, shrineSpirit, equipmentGolem, forestDryad, customType)
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val permanent = driver.putPermanentOnBattlefield(player, permanentName)
        val aura = driver.putCardInHand(player, auraName)
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, aura, listOf(permanent))
        driver.bothPass()
        return driver to permanent
    }

    test("losing creature keeps the enchantment's own subtype and drops the creature type") {
        val (driver, id) = enchant("Prune Probe Shrine Spirit", "Prune Probe Enchantment")
        val projected = driver.state.projectedState

        projected.isCreature(id) shouldBe false
        projected.hasType(id, "ENCHANTMENT") shouldBe true
        projected.hasSubtype(id, "Shrine") shouldBe true
        projected.hasSubtype(id, "Spirit") shouldBe false
        projected.isLegendary(id) shouldBe true
    }

    test("losing artifact and creature drops both their subtypes") {
        val (driver, id) = enchant("Prune Probe Equipment Golem", "Prune Probe Enchantment")
        val projected = driver.state.projectedState

        projected.hasType(id, "ARTIFACT") shouldBe false
        projected.hasSubtype(id, "Equipment") shouldBe false
        projected.hasSubtype(id, "Golem") shouldBe false
    }

    test("a land creature set to land keeps its land type and drops its creature type") {
        val (driver, id) = enchant("Prune Probe Forest Dryad", "Prune Probe Land")
        val projected = driver.state.projectedState

        projected.isCreature(id) shouldBe false
        projected.hasSubtype(id, "Forest") shouldBe true
        projected.hasSubtype(id, "Dryad") shouldBe false
    }

    test("creature types survive losing creature while the object is still kindred") {
        val (driver, id) = enchant("Prune Probe Shrine Spirit", "Prune Probe Kindred Enchantment")
        val projected = driver.state.projectedState

        projected.isCreature(id) shouldBe false
        projected.hasType(id, "KINDRED") shouldBe true
        projected.hasSubtype(id, "Spirit") shouldBe true
        projected.hasSubtype(id, "Shrine") shouldBe true
    }

    test("a subtype that is on no official list is left alone") {
        val (driver, id) = enchant("Prune Probe Custom Subtype", "Prune Probe Enchantment")

        driver.state.projectedState.hasSubtype(id, "Gloopwhatsit") shouldBe true
    }

    test("correlatedCardTypes follows the CR 205.3 subtype lists") {
        Subtype.correlatedCardTypes("Elf") shouldBe setOf(CardType.CREATURE, CardType.KINDRED)
        Subtype.correlatedCardTypes("Urza's") shouldBe setOf(CardType.LAND)
        Subtype.correlatedCardTypes("Vehicle") shouldBe setOf(CardType.ARTIFACT)
        Subtype.correlatedCardTypes("Saga") shouldBe setOf(CardType.ENCHANTMENT)
        Subtype.correlatedCardTypes("Jace") shouldBe setOf(CardType.PLANESWALKER)
        Subtype.correlatedCardTypes("Arcane") shouldBe setOf(CardType.INSTANT, CardType.SORCERY)
        Subtype.correlatedCardTypes("Siege") shouldBe setOf(CardType.BATTLE)
        Subtype.correlatedCardTypes("Gloopwhatsit") shouldBe emptySet()
    }
})
