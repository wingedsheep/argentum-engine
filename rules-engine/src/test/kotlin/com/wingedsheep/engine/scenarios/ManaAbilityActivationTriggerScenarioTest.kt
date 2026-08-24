package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine-level coverage for "whenever you activate an ability" firing off *mana* abilities.
 *
 * A mana ability is still an activated ability (CR 605.3) — it just resolves without using the
 * stack (CR 605.3b). So a trigger whose Oracle text carries no "that isn't a mana ability" clause
 * must see one. `EventPattern.AbilityActivatedEvent.includeManaAbilities` is that wording; the
 * default keeps the exclusion (Flamescroll Celebrant).
 *
 * These tests pin the *engine* rule rather than any one card, across both ways a mana ability gets
 * activated: the player tapping the source by hand, and the engine auto-tapping it to pay a cost.
 * The auto-tap fast path bypasses the activated-ability flow entirely — it taps sources straight
 * from the mana solver — so it is the half most likely to silently regress.
 */
class ManaAbilityActivationTriggerScenarioTest : FunSpec({

    // "Whenever a player activates a land's ability, you gain 1 life" — no mana-ability clause,
    // so a land's {T}: Add {R} counts. Scoped to lands so nothing else in the scenario fires it.
    val manaWatcher = card("Mana Watcher") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        oracleText = "Whenever a player activates a land's ability, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.activatesAbilityOf(
                GameObjectFilter.Land,
                player = Player.Each,
                includeManaAbilities = true
            )
            effect = Effects.GainLife(1)
        }
    }

    // The same watcher without the flag: the default "that isn't a mana ability" wording, which
    // must stay blind to the very same activation.
    val nonManaWatcher = card("Non-Mana Watcher") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        oracleText = "Whenever a player activates a land's ability that isn't a mana ability, " +
            "you gain 1 life."
        triggeredAbility {
            trigger = Triggers.activatesAbilityOf(GameObjectFilter.Land, player = Player.Each)
            effect = Effects.GainLife(1)
        }
    }

    // A {1} sink with no tap cost, so paying it forces the engine to auto-tap a land.
    val manaSink = card("Mana Sink") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "{1}: Draw a card."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.DrawCards(1)
        }
    }

    // A land whose mana ability is reachable by name from the registry.
    val tapLand = card("Quiet Waste") {
        typeLine = "Land"
        oracleText = "{T}: Add {C}."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = AddColorlessManaEffect(1)
            manaAbility = true
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(manaWatcher, nonManaWatcher, manaSink, tapLand).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 30) {
            bothPass(); guard++
        }
    }

    test("tapping a land for mana by hand fires an includeManaAbilities trigger") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Mana Watcher")
        val land = driver.putLandOnBattlefield(me, "Quiet Waste")

        val lifeBefore = driver.getLifeTotal(me)
        val abilityId = driver.cardRegistry.requireCard("Quiet Waste").activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = land, abilityId = abilityId))
        driver.resolveStack()

        driver.getLifeTotal(me) shouldBe lifeBefore + 1
    }

    test("the default wording stays blind to that same mana ability") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Non-Mana Watcher")
        val land = driver.putLandOnBattlefield(me, "Quiet Waste")

        val lifeBefore = driver.getLifeTotal(me)
        val abilityId = driver.cardRegistry.requireCard("Quiet Waste").activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = land, abilityId = abilityId))
        driver.resolveStack()

        driver.getLifeTotal(me) shouldBe lifeBefore
    }

    test("auto-tapping a land to pay an ability cost fires it too") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Mana Watcher")
        val sink = driver.putPermanentOnBattlefield(me, "Mana Sink")
        driver.putLandOnBattlefield(me, "Quiet Waste")

        val lifeBefore = driver.getLifeTotal(me)
        // No floating mana: the {1} can only come from the engine tapping the land itself.
        val abilityId = driver.cardRegistry.requireCard("Mana Sink").activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = sink, abilityId = abilityId))
        driver.resolveStack()

        driver.getLifeTotal(me) shouldBe lifeBefore + 1
    }
})
