package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [ModifyLifeGain] — "you gain that much life plus 1 instead", "twice that much life" — on every
 * life-gain path:
 * - a source that has lost all its abilities stops modifying life gain (CR 613.1f, layer 6);
 * - modifiers whose order changes the result (a doubler next to a "+1") let the affected player
 *   choose which applies first (CR 616.1), while same-shape stacks apply without a prompt.
 */
class LifeGainModifierTest : ScenarioTestBase() {
    private fun enchantment(name: String, replacement: ReplacementEffect) = card(name) {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(replacement)
    }
    private fun spell(name: String, body: com.wingedsheep.sdk.scripting.effects.Effect) = card(name) {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = body }
    }

    private val doubler = enchantment("Test Modifier Doubler", ModifyLifeGain(multiplier = 2))
    private val plusOne = enchantment("Test Modifier Plus One",
        ModifyLifeGain(multiplier = 1, modifier = 1, appliesTo = EventPattern.LifeGainEvent(Player.You)))
    private val gainThree = spell("Test Modifier Gain Three", Effects.GainLife(3))
    private val erase = card("Test Modifier Erase") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val permanent = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(permanent) }
    }
    private val lifelinkBurn = card("Test Modifier Lifelink Burn") {
        manaCost = "{0}"
        typeLine = "Instant"
        keywords(Keyword.LIFELINK)
        spell { effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }
    private val lifelinkBear = card("Test Modifier Lifelink Bear") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.LIFELINK)
    }

    private fun board(permanents: List<String>, hand: List<String>): TestGame {
        val builder = scenario().withPlayers("Player1", "Player2")
            .withActivePlayer(1).withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        permanents.forEach { builder.withCardOnBattlefield(1, it) }
        hand.forEach { builder.withCardInHand(1, it) }
        repeat(10) {
            builder.withCardInLibrary(1, "Island")
            builder.withCardInLibrary(2, "Island")
        }
        return builder.build()
    }

    private fun resolve(game: TestGame, spell: String) {
        game.castSpell(1, spell).error shouldBe null
        game.resolveStack()
    }

    private fun chooseFirst(game: TestGame, name: String) {
        val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe game.player1Id
        val index = decision.options.indexOfFirst { it.startsWith(name) }
        (index >= 0) shouldBe true
        game.submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
        game.resolveStack()
    }

    init {
        listOf(doubler, plusOne, gainThree, erase, lifelinkBurn, lifelinkBear).forEach(cardRegistry::register)

        test("a lone plus-one modifier adds one to the gain") {
            val game = board(listOf(plusOne.name), listOf(gainThree.name))
            resolve(game, gainThree.name)
            game.getLifeTotal(1) shouldBe 24
        }

        test("a modifier whose source lost all abilities no longer applies") {
            val game = board(listOf(plusOne.name), listOf(erase.name, gainThree.name))
            game.castSpell(1, erase.name, game.findPermanent(plusOne.name)!!).error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasLostAllAbilities(game.findPermanent(plusOne.name)!!) shouldBe true
            resolve(game, gainThree.name)
            game.getLifeTotal(1) shouldBe 23
        }

        test("an ability-less modifier leaves no order to choose beside a live one") {
            val game = board(listOf(plusOne.name, doubler.name), listOf(erase.name, gainThree.name))
            game.castSpell(1, erase.name, game.findPermanent(plusOne.name)!!).error shouldBe null
            game.resolveStack()
            resolve(game, gainThree.name)
            game.state.pendingDecision shouldBe null
            game.getLifeTotal(1) shouldBe 26
        }

        test("two plus-one modifiers stack without a prompt") {
            val game = board(listOf(plusOne.name, plusOne.name), listOf(gainThree.name))
            resolve(game, gainThree.name)
            game.state.pendingDecision shouldBe null
            game.getLifeTotal(1) shouldBe 25
        }

        test("two doublers stack without a prompt") {
            val game = board(listOf(doubler.name, doubler.name), listOf(gainThree.name))
            resolve(game, gainThree.name)
            game.state.pendingDecision shouldBe null
            game.getLifeTotal(1) shouldBe 32
        }

        for ((first, expected) in listOf(plusOne.name to 28, doubler.name to 27)) {
            test("the gaining player orders a doubler and a plus-one: $first first gains ${expected - 20}") {
                val game = board(listOf(doubler.name, plusOne.name), listOf(gainThree.name))
                resolve(game, gainThree.name)
                chooseFirst(game, first)
                game.state.pendingDecision shouldBe null
                game.getLifeTotal(1) shouldBe expected
            }
        }

        test("noncombat lifelink offers the same order choice") {
            val game = board(listOf(doubler.name, plusOne.name), listOf(lifelinkBurn.name))
            resolve(game, lifelinkBurn.name)
            chooseFirst(game, plusOne.name)
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 28
        }

        test("combat lifelink offers the same order choice") {
            val game = board(listOf(doubler.name, plusOne.name, lifelinkBear.name), emptyList())
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(lifelinkBear.name to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            var guard = 0
            while (game.state.pendingDecision !is ChooseOptionDecision && guard++ < 10) game.passPriority()
            chooseFirst(game, doubler.name)
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 25
        }
    }
}
