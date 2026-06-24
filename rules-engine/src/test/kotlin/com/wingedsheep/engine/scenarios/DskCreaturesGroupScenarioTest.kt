package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario coverage for the DSK creatures batch implemented from existing SDK primitives:
 *
 *  - Most Valuable Slayer ({3}{R} 2/4) — "Whenever you attack, target attacking creature gets
 *    +1/+0 and gains first strike until end of turn."
 *  - Shroudstomper ({3}{W}{W}{B}{B} 5/5, deathtouch) — "Whenever this creature enters or attacks,
 *    each opponent loses 2 life. You gain 2 life and draw a card."
 *  - Hand That Feeds ({1}{R} 2/2) — "Delirium — Whenever this creature attacks while there are
 *    four or more card types among cards in your graveyard, it gets +2/+0 and gains menace until
 *    end of turn."
 *
 * All three reuse existing effects/triggers/conditions, so this Kotest scenario net proves the
 * composed behaviour rather than any new engine mechanic.
 */
class DskCreaturesGroupScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Most Valuable Slayer — attack trigger buffs a target attacker") {

            test("target attacking creature gets +1/+0 and first strike until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Most Valuable Slayer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Base 2/2 before the attack trigger") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Most Valuable Slayer" to 2, "Grizzly Bears" to 2)
                ).error shouldBe null

                withClue("YouAttack trigger should ask for its attacking-creature target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Targeted attacker got +1/+0 and first strike") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 2
                    game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
                }
            }
        }

        context("Shroudstomper — enters/attacks drain, gain, and draw") {

            test("entering drains each opponent 2, gains 2, and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withCardInHand(1, "Shroudstomper")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                // Cast Shroudstomper; its enters trigger fires after it resolves.
                game.castSpell(1, "Shroudstomper").error shouldBe null
                game.resolveStack()

                withClue("Each opponent lost 2 life") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("Controller gained 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("Controller drew a card (hand was the spell only, now +1 from draw)") {
                    // Spell left hand (-1), enters trigger drew one card (+1) => net == handBefore.
                    game.handSize(1) shouldBe handBefore
                }

                val stomper = game.findPermanent("Shroudstomper")!!
                withClue("Has deathtouch") {
                    game.state.projectedState.hasKeyword(stomper, Keyword.DEATHTOUCH) shouldBe true
                }
            }

            test("attacking drains each opponent 2, gains 2, and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Shroudstomper", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                val handBefore = game.handSize(1)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Shroudstomper" to 2)).error shouldBe null
                game.resolveStack()

                withClue("Attacks payoff: opponent -2, controller +2, draw 1") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 22
                    game.handSize(1) shouldBe (handBefore + 1)
                }
            }
        }

        context("Hand That Feeds — Delirium attack buff") {

            test("with fewer than four card types in graveyard, attacking does nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hand That Feeds", summoningSickness = false)
                    // Only three card types: creature, instant, sorcery.
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Doom Blade")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                val hand = game.findPermanent("Hand That Feeds")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hand That Feeds" to 2)).error shouldBe null
                game.resolveStack()

                withClue("Delirium off — vanilla 2/2, no menace") {
                    game.state.projectedState.getPower(hand) shouldBe 2
                    game.state.projectedState.getToughness(hand) shouldBe 2
                    game.state.projectedState.hasKeyword(hand, Keyword.MENACE) shouldBe false
                }
            }

            test("with four or more card types in graveyard, attacking grants +2/+0 and menace") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hand That Feeds", summoningSickness = false)
                    // Four card types: creature, instant, sorcery, enchantment.
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Doom Blade")
                    .withCardInGraveyard(1, "Test Enchantment")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                val hand = game.findPermanent("Hand That Feeds")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hand That Feeds" to 2)).error shouldBe null
                game.resolveStack()

                withClue("Delirium on — 2/2 + 2/0 = 4/2 with menace") {
                    game.state.projectedState.getPower(hand) shouldBe 4
                    game.state.projectedState.getToughness(hand) shouldBe 2
                    game.state.projectedState.hasKeyword(hand, Keyword.MENACE) shouldBe true
                }
            }
        }
    }
}
