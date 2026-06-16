package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.GainControlByMostEffect
import com.wingedsheep.sdk.scripting.effects.PlayerRankMetric
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Guardian Beast (Arabian Nights, {3}{B} 2/4 Beast).
 *
 * "As long as this creature is untapped, noncreature artifacts you control can't be enchanted,
 * they have indestructible, and other players can't gain control of them."
 *
 * Covers all three granted properties (indestructible, can't-be-enchanted, can't-gain-control)
 * and that they switch off when Guardian Beast is tapped.
 */
class GuardianBeastScenarioTest : FunSpec({

    // A vanilla noncreature artifact to protect.
    val testRelic = card("Test Relic") {
        manaCost = "{0}"
        typeLine = "Artifact"
    }
    val smashRelic = CardDefinition.sorcery(
        name = "Smash Relic",
        manaCost = ManaCost.parse("{1}"),
        oracleText = "Destroy target artifact.",
        script = CardScript.spell(Effects.Destroy(EffectTarget.ContextTarget(0)), Targets.Artifact),
    )
    val seizeRelic = CardDefinition.sorcery(
        name = "Seize Relic",
        manaCost = ManaCost.parse("{1}"),
        oracleText = "Gain control of target artifact.",
        script = CardScript.spell(Effects.GainControl(EffectTarget.ContextTarget(0)), Targets.Artifact),
    )
    // Exchange control of two target artifacts (exercises ExchangeControlExecutor).
    val swapRelics = CardDefinition.sorcery(
        name = "Swap Relics",
        manaCost = ManaCost.parse("{1}"),
        oracleText = "Exchange control of two target artifacts.",
        script = CardScript.spell(
            Effects.ExchangeControl(EffectTarget.ContextTarget(0), EffectTarget.ContextTarget(1)),
            Targets.Artifact,
            Targets.Artifact,
        ),
    )
    // The player with the most life gains control of target artifact (exercises GainControlByMostExecutor).
    val richestTakesRelic = CardDefinition.sorcery(
        name = "Richest Takes Relic",
        manaCost = ManaCost.parse("{1}"),
        oracleText = "The player with the most life gains control of target artifact.",
        script = CardScript.spell(
            GainControlByMostEffect(PlayerRankMetric.LifeTotal, EffectTarget.ContextTarget(0)),
            Targets.Artifact,
        ),
    )
    // An Aura that enchants artifacts.
    val relicChains = card("Relic Chains") {
        manaCost = "{1}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant artifact"
        auraTarget = Targets.Artifact
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all +
                listOf(testRelic, smashRelic, seizeRelic, swapRelics, richestTakesRelic, relicChains),
        )
        return driver
    }

    test("noncreature artifacts you control are indestructible while Guardian Beast is untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val beast = driver.putCreatureOnBattlefield(you, "Guardian Beast")
        val relic = driver.putPermanentOnBattlefield(you, "Test Relic")

        // Untapped Guardian Beast: a "destroy target artifact" spell fails to destroy the relic.
        val smash1 = driver.putCardInHand(you, "Smash Relic")
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.BLACK, 1)
        driver.castSpellWithTargets(you, smash1, listOf(ChosenTarget.Permanent(relic)))
        driver.bothPass()
        driver.findPermanent(you, "Test Relic") shouldNotBe null

        // Tap Guardian Beast — the relic loses indestructible and is destroyed.
        driver.tapPermanent(beast)
        val smash2 = driver.putCardInHand(you, "Smash Relic")
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.BLACK, 1)
        driver.castSpellWithTargets(you, smash2, listOf(ChosenTarget.Permanent(relic)))
        driver.bothPass()
        driver.findPermanent(you, "Test Relic") shouldBe null
    }

    test("other players can't gain control of your noncreature artifacts while Guardian Beast is untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val beast = driver.putCreatureOnBattlefield(you, "Guardian Beast")
        val relic = driver.putPermanentOnBattlefield(you, "Test Relic")

        // Advance to the opponent's turn so they can cast a sorcery-speed steal.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (driver.activePlayer != opponent) {
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }

        val seize = driver.putCardInHand(opponent, "Seize Relic")
        driver.giveMana(opponent, com.wingedsheep.sdk.core.Color.BLACK, 1)
        driver.castSpellWithTargets(opponent, seize, listOf(ChosenTarget.Permanent(relic)))
        driver.bothPass()

        // Guardian Beast untapped → control didn't change.
        driver.state.getEntity(relic)?.get<ControllerComponent>()?.playerId shouldBe you
        driver.state.projectedState.getController(relic) shouldBe you
        beast shouldNotBe null
    }

    test("noncreature artifacts you control can't be enchanted while Guardian Beast is untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Guardian Beast")
        val relic = driver.putPermanentOnBattlefield(you, "Test Relic")

        // Casting an Aura targeting the protected relic is illegal.
        val aura = driver.putCardInHand(you, "Relic Chains")
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.BLACK, 1)
        val result = driver.castSpellWithTargets(you, aura, listOf(ChosenTarget.Permanent(relic)))
        result.error shouldNotBe null
    }

    test("an exchange-control effect can't swap away a protected artifact (the whole exchange fails)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.putCreatureOnBattlefield(you, "Guardian Beast")
        val yourRelic = driver.putPermanentOnBattlefield(you, "Test Relic")
        val theirRelic = driver.putPermanentOnBattlefield(opponent, "Test Relic")

        // Opponent casts an exchange targeting your protected relic and their own.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (driver.activePlayer != opponent) {
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val swap = driver.putCardInHand(opponent, "Swap Relics")
        driver.giveMana(opponent, com.wingedsheep.sdk.core.Color.BLACK, 1)
        driver.castSpellWithTargets(
            opponent,
            swap,
            listOf(ChosenTarget.Permanent(yourRelic), ChosenTarget.Permanent(theirRelic)),
        )
        driver.bothPass()

        // The whole exchange fails: neither permanent changed hands.
        driver.state.projectedState.getController(yourRelic) shouldBe you
        driver.state.projectedState.getController(theirRelic) shouldBe opponent
    }

    test("a 'most life' control effect can't take a protected artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Guardian Beast")
        val relic = driver.putPermanentOnBattlefield(you, "Test Relic")

        // Make the opponent the unique most-life player, then resolve "most life gains control".
        driver.setLifeTotal(opponent, 30)
        val grab = driver.putCardInHand(you, "Richest Takes Relic")
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.BLACK, 1)
        driver.castSpellWithTargets(you, grab, listOf(ChosenTarget.Permanent(relic)))
        driver.bothPass()

        // Opponent would otherwise gain control; Guardian Beast prevents it.
        driver.state.projectedState.getController(relic) shouldBe you
    }

    test("Auras already attached are not removed when Guardian Beast becomes untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val beast = driver.putCreatureOnBattlefield(you, "Guardian Beast")
        val relic = driver.putPermanentOnBattlefield(you, "Test Relic")

        // While Guardian Beast is tapped the relic can be enchanted, so attach an Aura.
        driver.tapPermanent(beast)
        val aura = driver.putCardInHand(you, "Relic Chains")
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.BLACK, 1)
        driver.castSpellWithTargets(you, aura, listOf(ChosenTarget.Permanent(relic)))
        driver.bothPass()
        val auraId = driver.findPermanent(you, "Relic Chains")
        driver.state.getEntity(auraId!!)?.get<AttachedToComponent>()?.targetId shouldBe relic

        // Untapping Guardian Beast re-enables "can't be enchanted" but must NOT detach the Aura.
        driver.untapPermanent(beast)
        driver.state.getEntity(auraId)?.get<AttachedToComponent>()?.targetId shouldBe relic
    }
})
