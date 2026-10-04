package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A token has no mana cost "unless the effect that creates them specifies otherwise" (CR 202.1b),
 * and the characteristics its creator defines are functionally its printed ones (CR 111.3). A
 * predefined token minted from a definition that *has* a mana cost — Ral and the Implicit Maze's
 * Spellgorger Weird, a copy of the Oracle card — carries that cost, so its mana value and its
 * colors come from it. A predefined token without one (Treasure) still has mana value 0.
 */
class PredefinedTokenManaCostScenarioTest : ScenarioTestBase() {

    private val costedToken = card("Costed Test Weird") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Weird"
        power = 2
        toughness = 2
    }

    private val makeCostedToken = card("Make Costed Weird") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreatePredefinedToken("Costed Test Weird") }
    }

    private val makeTreasure = card("Make Test Treasure") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateTreasure() }
    }

    // "Destroy each creature with mana value 3 or greater" — reads the token's mana value.
    private val sweepThreePlus = card("Sweep Mana Value Three") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Patterns.Group.destroyAll(GroupFilter(GameObjectFilter.Creature.manaValueAtLeast(3)))
        }
    }

    // Peregrin Took's clause, minting the costed token as the "additional" one.
    private val additionalCostedMaker = card("Additional Costed Maker") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Halfling"
        power = 1
        toughness = 1
        replacementEffect(CreateAdditionalToken(additionalTokenType = "Costed Test Weird"))
    }

    private fun card(game: TestGame, name: String): CardComponent =
        game.state.getEntity(game.findPermanent(name)!!)!!.get<CardComponent>()!!

    private fun inMain(vararg hand: String, onBattlefield: List<String> = emptyList()): TestGame {
        val builder = scenario()
            .withPlayers("Alice", "Bob")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        hand.forEach { builder.withCardInHand(1, it) }
        onBattlefield.forEach { builder.withCardOnBattlefield(1, it) }
        return builder.build()
    }

    init {
        cardRegistry.register(listOf(costedToken, makeCostedToken, makeTreasure, sweepThreePlus, additionalCostedMaker))

        test("a predefined token keeps its definition's mana cost, mana value and colors") {
            val game = inMain("Make Costed Weird")
            game.castSpell(1, "Make Costed Weird").error shouldBe null
            game.resolveStack()

            val weirdId = game.findPermanent("Costed Test Weird")!!
            game.state.getEntity(weirdId)!!.has<TokenComponent>() shouldBe true
            val weird = card(game, "Costed Test Weird")
            weird.manaCost.toString() shouldBe "{2}{R}"
            weird.manaCost.cmc shouldBe 3
            game.state.projectedState.getColors(weirdId) shouldBe setOf(Color.RED.name)
        }

        test("a predefined token with no mana cost still has mana value 0 and no color") {
            val game = inMain("Make Test Treasure")
            game.castSpell(1, "Make Test Treasure").error shouldBe null
            game.resolveStack()

            val treasure = card(game, "Treasure")
            treasure.manaCost.cmc shouldBe 0
            game.state.projectedState.getColors(game.findPermanent("Treasure")!!) shouldBe emptySet()
        }

        test("a mana-value filter sees the token's mana value") {
            val game = inMain("Make Costed Weird", "Sweep Mana Value Three")
            game.castSpell(1, "Make Costed Weird").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Sweep Mana Value Three").error shouldBe null
            game.resolveStack()

            withClue("mana value 3 meets 'mana value 3 or greater'") {
                game.findPermanent("Costed Test Weird") shouldBe null
            }
        }

        test("an additional-token replacement mints the costed token with its mana cost") {
            val game = inMain("Make Test Treasure", onBattlefield = listOf("Additional Costed Maker"))
            game.castSpell(1, "Make Test Treasure").error shouldBe null
            game.resolveStack()

            card(game, "Costed Test Weird").manaCost.cmc shouldBe 3
        }
    }
}
