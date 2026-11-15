package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `ModifySpellCost.castUsing` — "Dash costs you pay cost {2} less" (Warbringer), "Flashback costs
 * you pay cost {2} less" (Catalyst Stone). The modifier names one of a spell's alternative costs
 * (CR 118.9), so it lands on that cast only: never on the same card cast for its mana cost, never
 * on another alternative cost. The offer the player sees and the cost the cast is charged agree.
 */
class CastUsingCostModifierTest : FunSpec({

    fun discounter(name: String, keyword: Keyword) = card(name) {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGeneric(2),
                castUsing = keyword,
            )
        }
    }

    val dashDiscounter = discounter("Dash Discounter", Keyword.DASH)
    val flashbackDiscounter = discounter("Flashback Discounter", Keyword.FLASHBACK)

    val dasher = card("Discount Dasher") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Orc"
        power = 3
        toughness = 3
        dash = "{2}{R}"
    }

    val flashbackDraw = card("Discount Flashback Draw") {
        manaCost = "{2}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(1) }
        keywordAbility(KeywordAbility.flashback("{3}{R}"))
    }

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(dashDiscounter, flashbackDiscounter, dasher, flashbackDraw))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.offer(me: EntityId, card: EntityId, actionType: String): LegalAction? =
        legalActions(me).firstOrNull { (it.action as? CastSpell)?.cardId == card && it.actionType == actionType }

    fun GameTestDriver.dash(me: EntityId, card: EntityId) = submit(
        CastSpell(me, card, useAlternativeCost = true, alternativeCostType = AlternativeCostType.DASH)
    )

    test("a dash discount prices and charges the dash cast, not the mana-cost cast") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Dash Discounter")
        val card = driver.putCardInHand(me, "Discount Dasher")
        driver.putLandOnBattlefield(me, "Mountain")

        driver.offer(me, card, "CastWithDash").shouldNotBeNull().manaCostString shouldBe "{R}"
        driver.offer(me, card, "CastSpell").shouldNotBeNull().manaCostString shouldBe "{3}{R}"
        driver.dash(me, card).outcome shouldBe Outcome.Done
    }

    test("a dash discount doesn't reach a flashback cast, and a flashback discount doesn't reach dash") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Dash Discounter")
        val spell = driver.putCardInGraveyard(me, "Discount Flashback Draw")
        val creature = driver.putCardInHand(me, "Discount Dasher")
        repeat(2) { driver.putLandOnBattlefield(me, "Mountain") }
        fun flashback() = driver.submit(
            CastSpell(me, spell, useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK)
        )

        driver.offer(me, spell, "CastWithFlashback").shouldNotBeNull().manaCostString shouldBe "{3}{R}"
        // Two Mountains pay {1}{R} — a dash discount leaking onto flashback would make this cast legal.
        flashback().outcome shouldNotBe Outcome.Done
        driver.putPermanentOnBattlefield(me, "Flashback Discounter")
        driver.offer(me, spell, "CastWithFlashback").shouldNotBeNull().manaCostString shouldBe "{1}{R}"
        driver.offer(me, creature, "CastWithDash").shouldNotBeNull().manaCostString shouldBe "{R}"
        flashback().outcome shouldBe Outcome.Done
    }

    test("every keyword the SDK accepts is one the engine prices, and no other") {
        AlternativeCostType.entries.mapNotNull { it.castUsing }.toSet() shouldBe ModifySpellCost.CAST_USING_KEYWORDS
    }

    test("an opponent's dash discount doesn't reduce your dash cost") {
        val driver = newGame()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Dash Discounter")
        val card = driver.putCardInHand(me, "Discount Dasher")
        driver.putLandOnBattlefield(me, "Mountain")

        driver.offer(me, card, "CastWithDash").shouldNotBeNull().manaCostString shouldBe "{2}{R}"
        driver.dash(me, card).outcome shouldNotBe Outcome.Done
    }

    test("it reads as the printed text and rejects a keyword that names no alternative cost") {
        ModifySpellCost(
            SpellCostTarget.YouCast(GameObjectFilter.Any), CostModification.ReduceGeneric(2), castUsing = Keyword.DASH,
        ).description shouldBe "Dash costs you pay cost {2} less"
        shouldThrow<IllegalArgumentException> {
            ModifySpellCost(
                SpellCostTarget.YouCast(GameObjectFilter.Any), CostModification.ReduceGeneric(1), castUsing = Keyword.FLYING,
            )
        }
        shouldThrow<IllegalArgumentException> {
            ModifySpellCost(
                SpellCostTarget.YouCast(GameObjectFilter.Any), CostModification.ReduceGeneric(1),
                optionalLifePayment = 2, castUsing = Keyword.DASH,
            )
        }
    }
})
