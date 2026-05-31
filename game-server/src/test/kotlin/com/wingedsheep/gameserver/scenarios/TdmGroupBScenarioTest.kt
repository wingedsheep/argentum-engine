package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for TDM "group B":
 *  - Defibrillating Current (#177): 4 damage to a creature/planeswalker + gain 2 life.
 *  - Seize Opportunity (#119): modal — impulse-exile top two OR pump up to two creatures.
 *  - Piercing Exhale (#151): fight-style; optional behold a Dragon → surveil 2.
 *  - Osseous Exhale (#17): 5 damage to attacking/blocking creature; behold → gain 2 life.
 *  - Dispelling Exhale (#41): counter unless pays {2}, or {4} if a Dragon was beheld.
 *
 * The three Exhale cards model the optional "you may behold a Dragon" additional cost at
 * resolution time (a MayEffect that stores the chosen Dragon), so after the spell resolves
 * the controller answers a yes/no and — if yes — selects the Dragon to behold.
 */
class TdmGroupBScenarioTest : ScenarioTestBase() {

    init {
        context("Defibrillating Current") {
            test("deals 4 damage to a creature and the caster gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Defibrillating Current")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Defibrillating Current", target)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant (3/3) takes 4 damage and dies") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Caster gains 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }

        context("Seize Opportunity") {
            test("mode 1 exiles the top two cards and lets you play them") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Seize Opportunity")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellWithMode(1, "Seize Opportunity", 0)
                withClue("Cast (mode 1) should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Both top cards left the library") {
                    game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 0
                    game.findCardsInLibrary(1, "Hill Giant").size shouldBe 0
                }
            }

            test("mode 2 pumps up to two target creatures +2/+1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Seize Opportunity")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpellWithMode(1, "Seize Opportunity", 1, bear)
                withClue("Cast (mode 2) should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val bearCard = game.getClientState(1).cards.values.first { it.name == "Grizzly Bears" }
                withClue("Grizzly Bears is pumped to 4/3") {
                    bearCard.power shouldBe 4
                    bearCard.toughness shouldBe 3
                }
            }
        }

        context("Osseous Exhale") {
            test("5 damage to an attacking creature; behold a Dragon to gain 2 life") {
                // Player 1 attacks with their own creature, then (holding priority) casts
                // Osseous Exhale at it — "attacking creature" is a valid target regardless of
                // controller.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Osseous Exhale")
                    .withCardInHand(1, "Kilnmouth Dragon") // a Dragon to behold
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3 attacker
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 0))

                val attacker = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Osseous Exhale", attacker)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // Resolution-time behold: a "choose up to one Dragon" selection is pending.
                withClue("A behold card selection should be pending") {
                    game.hasPendingDecision() shouldBe true
                }
                val dragon = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Kilnmouth Dragon"
                }
                game.selectCards(listOf(dragon))
                game.resolveStack()

                withClue("Hill Giant (3/3) takes 5 damage and dies") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Beholding a Dragon grants 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }

        context("Piercing Exhale") {
            test("my creature deals damage equal to its power; declining behold skips surveil") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Piercing Exhale")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Hill Giant")      // 3/3 attacker source
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2 victim
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanent("Hill Giant")!!
                val victim = game.findPermanent("Grizzly Bears")!!
                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Piercing Exhale"
                }
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(victim))
                    )
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // No Dragon to behold (none in hand/play) → behold selection is empty/auto-skipped.
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }

                withClue("Grizzly Bears (2/2) takes 3 damage and dies") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }

        context("Dispelling Exhale") {
            test("counters a spell unless its controller pays {2} when no Dragon is beheld") {
                // Player 2 casts Grizzly Bears (tapping out); Player 1 responds with Dispelling
                // Exhale. With no Dragon beheld the tax is {2}, which the tapped-out Player 2
                // cannot pay, so the spell is countered.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dispelling Exhale")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gbCast = game.castSpell(2, "Grizzly Bears")
                withClue("Player 2's Grizzly Bears cast should succeed: ${gbCast.error}") {
                    gbCast.error shouldBe null
                }
                // Player 2 holds priority after casting; pass it to Player 1.
                game.passPriority()

                val cast = game.castSpellTargetingStackSpell(1, "Dispelling Exhale", "Grizzly Bears")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // No Dragon to behold → selection auto-skips; counter tax is {2}.
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }

                withClue("Grizzly Bears should be countered (not on battlefield)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
