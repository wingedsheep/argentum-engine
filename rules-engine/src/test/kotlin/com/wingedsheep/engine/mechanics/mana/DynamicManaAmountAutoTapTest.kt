package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Regression: the auto-tapper (ManaSolver) must count what a *dynamic* mana ability actually
 * produces — Elvish Archdruid: "{T}: Add {G} for each Elf you control".
 *
 * The amount on `AddManaEffect` / `AddColorlessManaEffect` / `AddDynamicManaEffect` is a
 * [DynamicAmount], and the solver only read it when it was a [DynamicAmount.Fixed], falling back to
 * "one mana per tap" for everything else. So an Archdruid with three Elves out was reported as a
 * single green source: spells it could pay for on its own were never highlighted as castable, and
 * auto-pay refused to cast them. The same undercount hit Gaea's Cradle, Tolarian Academy, Marwyn,
 * and Viridian Joiner. The fix evaluates the amount against current state (stable during a
 * payment), as the choice-based effects already did.
 */
class DynamicManaAmountAutoTapTest : FunSpec({

    /** The shared board: the driver, the Elf player, and the Archdruid's entity. */
    data class Board(val driver: GameTestDriver, val playerId: EntityId, val druidId: EntityId)

    // Faithful copy of Elvish Archdruid's mana ability (the lord half is irrelevant here).
    val archdruid = card("Test Elf Archdruid") {
        typeLine = "Creature — Elf Druid"
        manaCost = "{1}{G}{G}"
        colorIdentity = "G"
        power = 2
        toughness = 2
        oracleText = "{T}: Add {G} for each Elf you control."

        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(
                Color.GREEN,
                DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature.withSubtype(Subtype.ELF))
            )
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    // A vanilla Elf, purely to raise the Elf count the ability reads.
    val warrior = card("Test Elf Warrior") {
        typeLine = "Creature — Elf Warrior"
        manaCost = "{G}"
        colorIdentity = "G"
        power = 1
        toughness = 1
    }

    // A plain {2}{G} body — not an Elf, so casting it can't change the Elf count mid-payment.
    val bear = card("Test Payoff Bear") {
        typeLine = "Creature — Bear"
        manaCost = "{2}{G}"
        colorIdentity = "G"
        power = 3
        toughness = 3
    }

    // Gaea's Cradle, whose count can legitimately be zero — the other end of the same amount.
    val cradle = card("Test Legendary Cradle") {
        manaCost = ""
        colorIdentity = "G"
        typeLine = "Legendary Land"
        oracleText = "{T}: Add {G} for each creature you control."

        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(
                Color.GREEN,
                DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature)
            )
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    val allTestCards = TestCards.all + listOf(archdruid, warrior, bear, cradle)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allTestCards)
        return driver
    }

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(allTestCards)
        return registry
    }

    /** Archdruid plus two other Elves, all able to tap: one tap makes {G}{G}{G}. */
    fun boardWithThreeElves(): Board {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val playerId = driver.activePlayer!!
        val druidId = driver.putCreatureOnBattlefield(playerId, "Test Elf Archdruid")
        driver.removeSummoningSickness(druidId)
        repeat(2) {
            driver.removeSummoningSickness(driver.putCreatureOnBattlefield(playerId, "Test Elf Warrior"))
        }
        return Board(driver, playerId, druidId)
    }

    test("mana source reports the dynamic amount its ability produces") {
        val (driver, playerId) = boardWithThreeElves()

        val source = ManaSolver(createRegistry())
            .findAvailableManaSources(driver.state, playerId)
            .single { it.name == "Test Elf Archdruid" }

        source.producesColors shouldContain Color.GREEN
        source.manaAmount shouldBe 3
    }

    test("available mana count includes every mana a dynamic ability makes") {
        val (driver, playerId) = boardWithThreeElves()

        ManaSolver(createRegistry()).getAvailableManaCount(driver.state, playerId) shouldBe 3
    }

    test("a cost payable off one dynamic tap is solved by tapping that source alone") {
        val (driver, playerId) = boardWithThreeElves()

        val solution = ManaSolver(createRegistry())
            .solve(driver.state, playerId, ManaCost.parse("{2}{G}"))

        solution.shouldNotBeNull()
        solution.sources.map { it.name } shouldBe listOf("Test Elf Archdruid")
    }

    test("auto-pay floats every mana the tap makes, so the spell it planned for actually resolves") {
        val (driver, playerId, druidId) = boardWithThreeElves()
        val bearId = driver.putCardInHand(playerId, "Test Payoff Bear")

        // No lands on the battlefield: the {2}{G} can only come from one Archdruid tap.
        driver.castSpell(playerId, bearId)
        driver.bothPass()

        driver.state.getBattlefield(playerId) shouldContain bearId
        driver.state.getEntity(druidId)?.has<TappedComponent>() shouldBe true
        // Exactly {G}{G}{G} was produced and exactly {2}{G} spent — nothing left floating.
        (driver.state.getEntity(playerId)?.get<ManaPoolComponent>()?.green ?: 0) shouldBe 0
    }

    test("a land whose dynamic ability is dry right now is not offered as a mana source") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val playerId = driver.activePlayer!!
        driver.putLandOnBattlefield(playerId, "Test Legendary Cradle")

        val solver = ManaSolver(createRegistry())

        // "Add {G} for each creature you control" with no creatures adds nothing. The land must not
        // claim a green it can't make, and must not fall back to the colorless land default either.
        solver.findAvailableManaSources(driver.state, playerId)
            .map { it.name } shouldNotContain "Test Legendary Cradle"
        solver.getAvailableManaCount(driver.state, playerId) shouldBe 0
        solver.solve(driver.state, playerId, ManaCost.parse("{G}")).shouldBeNull()
        solver.solve(driver.state, playerId, ManaCost.parse("{1}")).shouldBeNull()
    }

    test("the same land is a full source again as soon as the count is nonzero") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val playerId = driver.activePlayer!!
        driver.putLandOnBattlefield(playerId, "Test Legendary Cradle")
        repeat(2) { driver.putCreatureOnBattlefield(playerId, "Test Elf Warrior") }

        val source = ManaSolver(createRegistry())
            .findAvailableManaSources(driver.state, playerId)
            .single { it.name == "Test Legendary Cradle" }

        source.producesColors shouldContain Color.GREEN
        source.manaAmount shouldBe 2
    }
})
