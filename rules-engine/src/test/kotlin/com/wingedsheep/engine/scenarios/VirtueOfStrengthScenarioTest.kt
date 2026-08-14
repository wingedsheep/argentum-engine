package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.Lavaleaper
import com.wingedsheep.mtg.sets.definitions.woe.cards.VirtueOfStrength
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Virtue of Strength — "If you tap a basic land for mana, it produces three times as much of that
 * mana instead." Covers the new `MultiplyManaOnSourceTap` static on both mana paths (manual tap and
 * the auto-tap solver) plus the three narrowings its rulings impose.
 *
 * Note the enchantment is put straight onto the battlefield rather than cast: `{5}{G}{G}` is
 * irrelevant to the static's behaviour and would need seven lands of setup per case.
 */
class VirtueOfStrengthScenarioTest : FunSpec({

    val TestForest = basicLand("Forest") {}
    val TestMountain = basicLand("Mountain") {}

    /**
     * A nonbasic land with the printed tap ability of a Forest. Virtue of Strength cares about the
     * *basic* supertype, so this is the negative control for the filter.
     */
    val TestNonbasicForest = card("Timber Grove") {
        manaCost = ""
        colorIdentity = "G"
        typeLine = "Land"
        oracleText = "{T}: Add {G}."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    val extraCards = listOf(TestForest, TestMountain, TestNonbasicForest, VirtueOfStrength, Lavaleaper)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extraCards)
        return driver
    }

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(TestCards.all + extraCards)
        return registry
    }

    test("tapping a basic Forest with Virtue of Strength out produces three green mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Virtue of Strength")
        val forest = driver.putPermanentOnBattlefield(player, "Forest")

        val result = driver.submit(
            ActivateAbility(player, forest, TestForest.activatedAbilities[0].id)
        )
        result.isSuccess shouldBe true

        driver.state.getEntity(player)?.get<ManaPoolComponent>()!!.green shouldBe 3
    }

    test("two copies are cumulative and multiplicative — nine mana, not six") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Virtue of Strength")
        driver.putPermanentOnBattlefield(player, "Virtue of Strength")
        val forest = driver.putPermanentOnBattlefield(player, "Forest")

        driver.submit(ActivateAbility(player, forest, TestForest.activatedAbilities[0].id))
            .isSuccess shouldBe true

        driver.state.getEntity(player)?.get<ManaPoolComponent>()!!.green shouldBe 9
    }

    test("only basic lands are multiplied") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Virtue of Strength")
        val grove = driver.putPermanentOnBattlefield(player, "Timber Grove")

        driver.submit(ActivateAbility(player, grove, TestNonbasicForest.activatedAbilities[0].id))
            .isSuccess shouldBe true

        driver.state.getEntity(player)?.get<ManaPoolComponent>()!!.green shouldBe 1
    }

    test("an opponent's Virtue of Strength does not multiply your land — the filter is 'you control'") {
        // Unlike Lavaleaper ("whenever *a player* taps a basic land"), Virtue of Strength only
        // touches lands its own controller controls, so the opponent's copy must not fire here.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(opponent, "Virtue of Strength")
        val forest = driver.putPermanentOnBattlefield(player, "Forest")

        driver.submit(ActivateAbility(player, forest, TestForest.activatedAbilities[0].id))
            .isSuccess shouldBe true

        driver.state.getEntity(player)?.get<ManaPoolComponent>()!!.green shouldBe 1

        val solver = ManaSolver(createRegistry())
        solver.canPay(driver.state, player, ManaCost.parse("{G}{G}")) shouldBe false
    }

    test("a separate triggered mana ability's bonus is not multiplied") {
        // Lavaleaper: "Whenever a player taps a basic land for mana, that player adds one mana of
        // any type that land produced." Per the rulings that bonus comes from a different ability,
        // so a tapped Forest yields 3 (multiplied) + 1 (bonus) = 4, not 3 × 2 = 6.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Virtue of Strength")
        driver.putCreatureOnBattlefield(player, "Lavaleaper")
        val forest = driver.putPermanentOnBattlefield(player, "Forest")

        driver.submit(ActivateAbility(player, forest, TestForest.activatedAbilities[0].id))
            .isSuccess shouldBe true

        driver.state.getEntity(player)?.get<ManaPoolComponent>()!!.green shouldBe 4
    }

    test("the auto-tap solver budgets with the tripled amount") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Virtue of Strength")
        driver.putPermanentOnBattlefield(player, "Forest")

        val solver = ManaSolver(createRegistry())
        solver.canPay(driver.state, player, ManaCost.parse("{G}{G}{G}")) shouldBe true
        solver.canPay(driver.state, player, ManaCost.parse("{G}{G}{G}{G}")) shouldBe false
    }

    test("without Virtue of Strength a basic Forest still produces one mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(player, "Forest")
        driver.submit(ActivateAbility(player, forest, TestForest.activatedAbilities[0].id))
            .isSuccess shouldBe true

        driver.state.getEntity(player)?.get<ManaPoolComponent>()!!.green shouldBe 1
    }
})
