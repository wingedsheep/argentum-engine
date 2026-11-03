package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Immortal Sun (RIX #180) — "Players can't activate planeswalkers' loyalty abilities. At the
 * beginning of your draw step, draw an additional card. Spells you cast cost {1} less to cast.
 * Creatures you control get +1/+1."
 *
 * The loyalty lock is `PlayersCantActivateAbilities(loyaltyAbilitiesOnly = true)`: it binds every
 * player, its controller included, but leaves a planeswalker's non-loyalty activated abilities alone.
 */
class TheImmortalSunScenarioTest : ScenarioTestBase() {

    // A planeswalker with a loyalty ability and a non-loyalty activated ability.
    private val testWalker = card("Sun Test Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Tester"
        startingLoyalty = 3
        loyaltyAbility(1) {
            effect = Effects.GainLife(1)
        }
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.GainLife(2)
        }
    }

    init {
        cardRegistry.register(testWalker)

        fun abilities() = cardRegistry.getCard("Sun Test Walker")!!.script.activatedAbilities

        test("no player can activate loyalty abilities; other planeswalker abilities stay activatable") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "The Immortal Sun")
                .withCardOnBattlefield(1, "Sun Test Walker")
                .withCardOnBattlefield(2, "Sun Test Walker")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val (loyalty, other) = abilities().let { a -> a.first { it.isPlaneswalkerAbility } to a.first { !it.isPlaneswalkerAbility } }
            val (mine, theirs) = game.findPermanents("Sun Test Walker").partition {
                game.state.projectedState.getController(it) == game.player1Id
            }.let { it.first.single() to it.second.single() }

            withClue("its controller's loyalty ability is neither offered nor accepted") {
                game.getLegalActions(1).any {
                    (it.action as? ActivateAbility)?.let { a -> a.sourceId == mine && a.abilityId == loyalty.id } == true
                } shouldBe false
                game.execute(ActivateAbility(game.player1Id, mine, loyalty.id)).error shouldNotBe null
            }
            withClue("a non-loyalty ability of the same planeswalker still activates") {
                game.getLegalActions(1).any {
                    (it.action as? ActivateAbility)?.let { a -> a.sourceId == mine && a.abilityId == other.id } == true
                } shouldBe true
                game.execute(ActivateAbility(game.player1Id, mine, other.id)).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 22
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id

            withClue("the opponent is locked out too") {
                game.execute(ActivateAbility(game.player2Id, theirs, loyalty.id)).error shouldNotBe null
            }
        }

        test("loyalty abilities come back once The Immortal Sun is gone") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sun Test Walker")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val loyalty = abilities().first { it.isPlaneswalkerAbility }
            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Sun Test Walker")!!, loyalty.id))
                .error shouldBe null
        }

        test("extra draw, cost reduction and anthem") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "The Immortal Sun")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("only its controller's creatures get +1/+1") {
                val (mine, theirs) = game.findPermanents("Grizzly Bears").partition {
                    game.state.projectedState.getController(it) == game.player1Id
                }
                game.state.projectedState.getPower(mine.single()) shouldBe 3
                game.state.projectedState.getToughness(mine.single()) shouldBe 3
                game.state.projectedState.getPower(theirs.single()) shouldBe 2
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            val handBefore = game.handSize(1)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id

            withClue("its controller draws two cards in their draw step") {
                game.handSize(1) shouldBe handBefore + 2
            }
            withClue("Grizzly Bears ({1}{G}) costs {G}, payable with one Forest") {
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.findPermanents("Grizzly Bears").size shouldBe 3
            }
        }
    }
}
