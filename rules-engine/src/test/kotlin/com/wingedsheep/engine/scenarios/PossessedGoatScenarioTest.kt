package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.dsk.cards.PossessedGoat
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Possessed Goat — specifically the additive "becomes a black Demon in addition to its
 * other colors and types" payoff. The activated ability puts three +1/+1 counters and applies an
 * additive color ([com.wingedsheep.sdk.scripting.effects.AddColorEffect]) + an additive creature
 * type, all permanent.
 *
 * The effect composition is exercised here via an inline instant (so we isolate the new
 * AddColorEffect executor and projection); the card definition's once-only activation is pinned by
 * a definition-level sanity check.
 */
class PossessedGoatScenarioTest : FunSpec({

    // Mirrors Possessed Goat's resolved ability: three +1/+1 counters, become black, become a Demon.
    val possess = card("Possess") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Put three +1/+1 counters on target creature and it becomes a black Demon " +
            "in addition to its other colors and types."
        spell {
            val target = target("target creature", Targets.Creature)
            effect = CompositeEffect(
                listOf(
                    Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, target),
                    Effects.AddColor(Color.BLACK, target, Duration.Permanent),
                    Effects.AddCreatureType("Demon", target, Duration.Permanent),
                )
            )
        }
    }

    val exorcise = card("Exorcise Goat") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "Target creature becomes a Goat."
        spell {
            val target = target("target creature", Targets.Creature)
            effect = Effects.SetCreatureSubtypes(setOf("Goat"), target, Duration.Permanent)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(possess, exorcise, PossessedGoat))
        return driver
    }

    test("becomes a black Demon with three +1/+1 counters, keeping its other colors and types") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Savannah Lions is a white Cat — used to prove colors/types are ADDED, not replaced.
        val creature = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val spell = driver.putCardInHand(player, "Possess")
        driver.giveMana(player, Color.WHITE, 1)
        driver.castSpell(player, spell, listOf(creature))
        driver.bothPass()

        // Three +1/+1 counters.
        driver.state.getEntity(creature)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3

        val projected = driver.state.projectedState
        // Black added, white kept (in addition to its other colors).
        projected.hasColor(creature, Color.BLACK) shouldBe true
        projected.hasColor(creature, Color.WHITE) shouldBe true
        // Demon added, Cat kept (in addition to its other types).
        projected.hasSubtype(creature, "Demon") shouldBe true
        projected.hasSubtype(creature, "Cat") shouldBe true
    }

    test("card definition: once-only ability with a mana + discard cost") {
        val ability = PossessedGoat.activatedAbilities.single()
        ability.restrictions shouldBe listOf(ActivationRestriction.Once)
        // The cost is the {3} + discard-a-card composite (no tap).
        ability.isManaAbility shouldBe false
    }

    test("uses possessed art only while Demon is among its projected creature types") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val goat = driver.putCreatureOnBattlefield(player, "Possessed Goat")
        val normalArt = PossessedGoat.metadata.imageUri
        val possessedArt = "/images/custom/possessed-goat.jpeg"

        fun displayedArt() =
            ClientStateTransformer(driver.cardRegistry)
                .transform(driver.state, viewingPlayerId = player)
                .cards[goat]
                ?.imageUri

        displayedArt() shouldBe normalArt

        val possession = driver.putCardInHand(player, "Possess")
        driver.giveMana(player, Color.WHITE, 1)
        driver.castSpell(player, possession, listOf(goat))
        driver.bothPass()

        driver.state.projectedState.hasSubtype(goat, "Demon") shouldBe true
        displayedArt() shouldBe possessedArt

        val exorcism = driver.putCardInHand(player, "Exorcise Goat")
        driver.giveMana(player, Color.WHITE, 1)
        driver.castSpell(player, exorcism, listOf(goat))
        driver.bothPass()

        driver.state.projectedState.hasSubtype(goat, "Demon") shouldBe false
        displayedArt() shouldBe normalArt
    }
})
