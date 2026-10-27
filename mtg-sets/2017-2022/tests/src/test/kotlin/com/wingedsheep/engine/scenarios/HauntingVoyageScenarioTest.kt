package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Haunting Voyage (KHM #98) — {4}{B}{B} Sorcery. "Choose a creature type. Return up to two creature
 * cards of that type from your graveyard to the battlefield. If this spell was foretold, return all
 * creature cards of that type from your graveyard to the battlefield instead." Foretell {5}{B}{B}.
 *
 * Cast from hand, the caster picks up to two cards of the chosen type (cards of other types are not
 * offered); foretold (CR 702.143c), every card of the chosen type returns without a pick.
 */
class HauntingVoyageScenarioTest : FunSpec({
    fun goblin(name: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1,
    )
    val elf = CardDefinition.creature(
        name = "Test Elf",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf")),
        power = 1,
        toughness = 1,
    )

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(goblin("Goblin A"), goblin("Goblin B"), goblin("Goblin C"), elf))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.chooseType(p: EntityId, type: String) {
        val decision = pendingDecision as ChooseOptionDecision
        submitDecision(p, OptionChosenResponse(decision.id, decision.options.indexOf(type))).error shouldBe null
    }

    fun GameTestDriver.fillGraveyard(p: EntityId): List<EntityId> {
        val goblins = listOf("Goblin A", "Goblin B", "Goblin C").map { putCardInGraveyard(p, it) }
        putCardInGraveyard(p, "Test Elf")
        return goblins
    }

    test("cast from hand, it returns up to two creature cards of the chosen type") {
        val d = driver()
        val p = d.activePlayer!!
        val goblins = d.fillGraveyard(p)
        val spell = d.putCardInHand(p, "Haunting Voyage")
        d.giveMana(p, Color.BLACK, 6)
        d.castSpell(p, spell).error shouldBe null
        d.bothPass()
        d.chooseType(p, "Goblin")

        val pick = d.pendingDecision as SelectCardsDecision
        pick.options shouldContainExactlyInAnyOrder goblins
        pick.minSelections shouldBe 0
        pick.maxSelections shouldBe 2
        d.submitDecision(p, CardsSelectedResponse(pick.id, goblins.take(2))).error shouldBe null

        (d.findPermanent(p, "Goblin A") != null) shouldBe true
        (d.findPermanent(p, "Goblin B") != null) shouldBe true
        d.findPermanent(p, "Goblin C") shouldBe null
        d.findPermanent(p, "Test Elf") shouldBe null
    }

    test("foretold and cast for its foretell cost, it returns every creature card of the chosen type") {
        val d = driver()
        val p = d.activePlayer!!
        val spell = d.putCardInHand(p, "Haunting Voyage")
        d.giveMana(p, Color.BLACK, 2)
        d.submitSuccess(ForetellCard(p, spell))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(p))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, p)
        d.fillGraveyard(p)
        d.giveMana(p, Color.BLACK, 7)
        d.castSpell(p, spell).error shouldBe null
        d.bothPass()
        d.chooseType(p, "Goblin")

        d.state.pendingDecision shouldBe null
        listOf("Goblin A", "Goblin B", "Goblin C").forEach { (d.findPermanent(p, it) != null) shouldBe true }
        d.findPermanent(p, "Test Elf") shouldBe null
    }
})
