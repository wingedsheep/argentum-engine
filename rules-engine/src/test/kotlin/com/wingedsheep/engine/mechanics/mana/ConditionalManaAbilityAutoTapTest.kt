package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Regression: the auto-tapper (ManaSolver) must recognize a *conditional* mana ability as a mana
 * source — Raucous Audience: "{T}: Add {G}. If you control a creature with power 4 or greater, add
 * {G}{G} instead."
 *
 * `ConditionalEffect` lowers to a [com.wingedsheep.sdk.scripting.effects.GatedEffect], which the
 * solver's effect-extraction did not handle — the gate fell through unread, the solver never saw
 * the green, and the source was silently skipped (player couldn't auto-tap it). The fix evaluates
 * the gate's condition against current state (stable during a payment) and reads the branch that
 * will actually run.
 */
class ConditionalManaAbilityAutoTapTest : FunSpec({

    // Faithful copy of Raucous Audience's conditional mana ability.
    val raucous = card("Raucous Tester") {
        typeLine = "Creature — Human Citizen"
        manaCost = "{1}{G}"
        colorIdentity = "G"
        power = 2
        toughness = 1
        oracleText = "{T}: Add {G}. If you control a creature with power 4 or greater, add {G}{G} instead."

        activatedAbility {
            cost = Costs.Tap
            effect = ConditionalEffect(
                condition = Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4)),
                effect = Effects.AddMana(Color.GREEN, 2),
                elseEffect = Effects.AddMana(Color.GREEN),
            )
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    // A vanilla 5/5 to flip the gate's "creature with power 4 or greater" condition on.
    val bruiser = CardDefinition.creature(
        name = "Big Bruiser",
        manaCost = ManaCost.parse("{4}{G}"),
        subtypes = setOf(com.wingedsheep.sdk.core.Subtype("Bear")),
        power = 5,
        toughness = 5,
    )

    val allCards = TestCards.all + listOf(raucous, bruiser)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    fun createRegistry(): CardRegistry = CardRegistry().also { it.register(allCards) }

    test("conditional mana ability is found as a green source (else branch — 1 green)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val player = driver.player1

        val src = driver.putCreatureOnBattlefield(player, "Raucous Tester")
        driver.removeSummoningSickness(src)

        val solution = ManaSolver(createRegistry()).solve(driver.state, player, ManaCost.parse("{G}"))

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 1
        solution.sources.map { it.name } shouldContain "Raucous Tester"
    }

    test("gate's then-branch is read: with a power-4+ creature it alone pays {G}{G}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val player = driver.player1

        val src = driver.putCreatureOnBattlefield(player, "Raucous Tester")
        driver.removeSummoningSickness(src)
        // Controlling a 5/5 flips the condition → the ability now yields {G}{G} from one tap.
        driver.putCreatureOnBattlefield(player, "Big Bruiser")

        val solution = ManaSolver(createRegistry()).solve(driver.state, player, ManaCost.parse("{G}{G}"))

        solution.shouldNotBeNull()
        // A single tap of Raucous Tester covers both green — no other green source needed.
        solution.sources shouldHaveSize 1
        solution.sources.map { it.name } shouldContain "Raucous Tester"
    }
})
