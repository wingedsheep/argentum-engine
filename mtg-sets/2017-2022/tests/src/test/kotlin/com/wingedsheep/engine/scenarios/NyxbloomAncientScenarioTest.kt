package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.BloomTender
import com.wingedsheep.mtg.sets.definitions.lea.cards.LlanowarElves
import com.wingedsheep.mtg.sets.definitions.thb.cards.NyxbloomAncient
import com.wingedsheep.mtg.sets.definitions.tla.cards.RaucousAudience
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Nyxbloom Ancient — "If you tap a permanent for mana, it produces three times as much of that
 * mana instead." Every shape of mana a tapped permanent makes is tripled: a land, a creature, a
 * conditional ability's resolving branch (Raucous Audience) and "one mana of each color among …"
 * (Bloom Tender), on the manual path and in the auto-tap solver; an opponent's Nyxbloom does not
 * touch your mana.
 */
class NyxbloomAncientScenarioTest : FunSpec({

    val TestForest = basicLand("Forest") {}
    val TestMountain = basicLand("Mountain") {}
    val RedCharm = card("Test Red Charm") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Enchantment"
    }

    fun comboTapper(name: String, colors: Set<Color>) = card(name) {
        manaCost = ""
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddManaInAnyCombination(2, colors)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }
    val GruulCombo = comboTapper("Test Gruul Combo", setOf(Color.RED, Color.GREEN))
    val RainbowCombo = comboTapper("Test Rainbow Combo", Color.entries.toSet())

    val extraCards = listOf(
        TestForest, TestMountain, RedCharm, GruulCombo, RainbowCombo, NyxbloomAncient, LlanowarElves, BloomTender, RaucousAudience,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extraCards)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun solver(): ManaSolver {
        val registry = CardRegistry()
        registry.register(TestCards.all + extraCards)
        return ManaSolver(registry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
    }

    fun GameTestDriver.pool(player: EntityId) =
        state.getEntity(player)?.get<ManaPoolComponent>()!!

    fun GameTestDriver.tapCreature(player: EntityId, name: String, abilityId: AbilityId) {
        val creature = putCreatureOnBattlefield(player, name)
        removeSummoningSickness(creature)
        submit(ActivateAbility(player, creature, abilityId)).outcome shouldBe Outcome.Done
    }

    test("a land you tap for mana produces three times as much") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Nyxbloom Ancient")
        val forest = driver.putPermanentOnBattlefield(player, "Forest")

        driver.submit(ActivateAbility(player, forest, TestForest.activatedAbilities[0].id))
            .outcome shouldBe Outcome.Done

        driver.pool(player).green shouldBe 3
    }

    test("a nonland permanent is a permanent too — Llanowar Elves makes three green") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Nyxbloom Ancient")

        driver.tapCreature(player, "Llanowar Elves", LlanowarElves.activatedAbilities[0].id)

        driver.pool(player).green shouldBe 3
    }

    test("one mana of each color among permanents is tripled per color — Bloom Tender") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Nyxbloom Ancient")
        driver.putPermanentOnBattlefield(player, "Test Red Charm")

        driver.tapCreature(player, "Bloom Tender", BloomTender.activatedAbilities[0].id)

        driver.pool(player).green shouldBe 3
        driver.pool(player).red shouldBe 3
    }

    test("a conditional mana ability's resolving branch is tripled — Raucous Audience") {
        // Nyxbloom is a 5/5, so Raucous Audience takes its "{G}{G} instead" branch: 2 × 3 = 6.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Nyxbloom Ancient")

        driver.tapCreature(player, "Raucous Audience", RaucousAudience.activatedAbilities[0].id)

        driver.pool(player).green shouldBe 6
    }

    test("two mana in any combination of {R}/{G}: the split is of two, then each unit is tripled") {
        // Ruling: the original amount and type are chosen, then tripled — so {R}{G} becomes
        // RRRGGG; a five-and-one split of six is not an option.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Nyxbloom Ancient")
        val combo = driver.putPermanentOnBattlefield(player, "Test Gruul Combo")

        driver.submit(ActivateAbility(player, combo, GruulCombo.activatedAbilities[0].id))
        val decision = driver.pendingDecision!!
        (decision as com.wingedsheep.engine.core.ChooseNumberDecision).maxValue shouldBe 2
        driver.submitDecision(player, NumberChosenResponse(decision.id, 1)).outcome shouldBe Outcome.Done

        driver.pool(player).red shouldBe 3
        driver.pool(player).green shouldBe 3
    }

    test("two mana in any combination of colors: two pips chosen, each tripled") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Nyxbloom Ancient")
        val combo = driver.putPermanentOnBattlefield(player, "Test Rainbow Combo")

        driver.submit(ActivateAbility(player, combo, RainbowCombo.activatedAbilities[0].id))
        driver.submitDecision(player, ColorChosenResponse(driver.pendingDecision!!.id, Color.BLUE))
        driver.submitDecision(player, ColorChosenResponse(driver.pendingDecision!!.id, Color.BLACK))
            .outcome shouldBe Outcome.Done

        driver.pendingDecision shouldBe null
        driver.pool(player).blue shouldBe 3
        driver.pool(player).black shouldBe 3
    }

    test("two Nyxblooms multiply together — nine times as much") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Nyxbloom Ancient")
        driver.putCreatureOnBattlefield(player, "Nyxbloom Ancient")
        val forest = driver.putPermanentOnBattlefield(player, "Forest")

        driver.submit(ActivateAbility(player, forest, TestForest.activatedAbilities[0].id))
            .outcome shouldBe Outcome.Done

        driver.pool(player).green shouldBe 9
    }

    test("an opponent's Nyxbloom Ancient leaves your mana alone") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(driver.getOpponent(player), "Nyxbloom Ancient")
        val forest = driver.putPermanentOnBattlefield(player, "Forest")

        driver.submit(ActivateAbility(player, forest, TestForest.activatedAbilities[0].id))
            .outcome shouldBe Outcome.Done

        driver.pool(player).green shouldBe 1
    }

    test("the auto-tap solver budgets with the tripled amount, conditional branch included") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Nyxbloom Ancient")
        val audience = driver.putCreatureOnBattlefield(player, "Raucous Audience")
        driver.removeSummoningSickness(audience)
        driver.putPermanentOnBattlefield(player, "Forest")

        solver().canPay(driver.state, player, ManaCost.parse("{8}{G}")) shouldBe true
        solver().canPay(driver.state, player, ManaCost.parse("{9}{G}")) shouldBe false
    }
})
