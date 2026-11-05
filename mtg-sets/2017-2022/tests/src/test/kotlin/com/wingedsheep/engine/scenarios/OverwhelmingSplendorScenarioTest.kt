package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.OverwhelmingSplendor
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Overwhelming Splendor (HOU #19) — "Enchant player. Creatures enchanted player controls lose all
 * abilities and have base power and toughness 1/1. Enchanted player can't activate abilities that
 * aren't mana abilities or loyalty abilities."
 *
 * Pins the `NON_MANA_NON_LOYALTY` activation scope and the `EnchantedPlayer` reach: the enchanted
 * player keeps mana and loyalty abilities, loses every other activated ability — a planeswalker's
 * non-loyalty one and cycling from hand included — and the Curse's controller is untouched.
 */
class OverwhelmingSplendorScenarioTest : ScenarioTestBase() {

    private val flyer = card("Splendor Test Flyer") {
        manaCost = "{3}"
        typeLine = "Creature — Bird"
        power = 3
        toughness = 4
        keywords(Keyword.FLYING)
    }

    private val relic = card("Splendor Test Relic") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
        }
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.GainLife(2)
        }
    }

    private val walker = card("Splendor Test Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Tester"
        startingLoyalty = 3
        loyaltyAbility(1) { effect = Effects.GainLife(1) }
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.GainLife(3)
        }
    }

    private val cycler = card("Splendor Test Cycler") {
        manaCost = "{5}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
        keywordAbility(KeywordAbility.cycling("{0}"))
    }

    init {
        cardRegistry.register(listOf(OverwhelmingSplendor, flyer, relic, walker, cycler))

        /** The Curse on player 1's battlefield enchanting player 2; both control the test permanents. */
        fun cursedOpponent(activePlayer: Int): TestGame {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Overwhelming Splendor")
                .withActivePlayer(activePlayer)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            for (p in 1..2) {
                builder.withCardOnBattlefield(p, "Splendor Test Flyer")
                    .withCardOnBattlefield(p, "Splendor Test Relic")
                    .withCardOnBattlefield(p, "Splendor Test Walker")
                    .withCardInHand(p, "Splendor Test Cycler")
                repeat(4) { builder.withCardInLibrary(p, "Plains") }
            }
            val game = builder.build()
            val curse = game.findPermanent("Overwhelming Splendor")!!
            game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(game.player2Id)) }
            return game
        }

        fun TestGame.owned(name: String, player: Int): com.wingedsheep.sdk.model.EntityId {
            val playerId = if (player == 1) player1Id else player2Id
            return findPermanents(name).single { state.projectedState.getController(it) == playerId }
        }

        fun abilities(name: String) = cardRegistry.getCard(name)!!.script.activatedAbilities

        test("enchanted player's creatures lose all abilities and become base 1/1; yours don't") {
            val game = cursedOpponent(activePlayer = 1)
            val projected = game.state.projectedState
            val theirs = game.owned("Splendor Test Flyer", 2)
            val mine = game.owned("Splendor Test Flyer", 1)

            projected.getPower(theirs) shouldBe 1
            projected.getToughness(theirs) shouldBe 1
            projected.hasKeyword(theirs, Keyword.FLYING) shouldBe false
            projected.getPower(mine) shouldBe 3
            projected.getToughness(mine) shouldBe 4
            projected.hasKeyword(mine, Keyword.FLYING) shouldBe true
        }

        test("enchanted player keeps mana and loyalty abilities but no others, in any zone") {
            val game = cursedOpponent(activePlayer = 2)
            val (relicMana, relicLife) = abilities("Splendor Test Relic").partition { it.isManaAbility }
                .let { it.first.single() to it.second.single() }
            val (loyalty, walkerLife) = abilities("Splendor Test Walker").partition { it.isPlaneswalkerAbility }
                .let { it.first.single() to it.second.single() }
            val relic = game.owned("Splendor Test Relic", 2)
            val walker = game.owned("Splendor Test Walker", 2)

            fun offered(source: com.wingedsheep.sdk.model.EntityId, abilityId: Any) =
                game.getLegalActions(2).any {
                    (it.action as? ActivateAbility)?.let { a -> a.sourceId == source && a.abilityId == abilityId } == true
                }

            withClue("a non-mana artifact ability is neither offered nor accepted") {
                offered(relic, relicLife.id) shouldBe false
                game.execute(ActivateAbility(game.player2Id, relic, relicLife.id)).error shouldNotBe null
            }
            withClue("a planeswalker's non-loyalty ability is forbidden") {
                offered(walker, walkerLife.id) shouldBe false
                game.execute(ActivateAbility(game.player2Id, walker, walkerLife.id)).error shouldNotBe null
            }
            withClue("cycling from hand is forbidden (the wording reaches every zone)") {
                val cycler = game.findCardsInHand(2, "Splendor Test Cycler").single()
                game.execute(CycleCard(game.player2Id, cycler)).error shouldNotBe null
            }
            withClue("the planeswalker's loyalty ability is still offered and activates") {
                offered(walker, loyalty.id) shouldBe true
                game.execute(ActivateAbility(game.player2Id, walker, loyalty.id)).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 21
            }
            withClue("a mana ability still activates") {
                game.execute(ActivateAbility(game.player2Id, relic, relicMana.id)).error shouldBe null
            }
        }

        test("the Curse's controller is not restricted") {
            val game = cursedOpponent(activePlayer = 1)
            val relicLife = abilities("Splendor Test Relic").single { !it.isManaAbility }
            val walkerLife = abilities("Splendor Test Walker").single { !it.isPlaneswalkerAbility }

            game.execute(ActivateAbility(game.player1Id, game.owned("Splendor Test Relic", 1), relicLife.id)).error shouldBe null
            game.resolveStack()
            game.execute(ActivateAbility(game.player1Id, game.owned("Splendor Test Walker", 1), walkerLife.id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 25
            val cycler = game.findCardsInHand(1, "Splendor Test Cycler").single()
            game.execute(CycleCard(game.player1Id, cycler)).error shouldBe null
        }
    }
}
