package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Coordinated Clobbering (DSK #173) — {G} Sorcery.
 *
 * "Tap one or two target untapped creatures you control. They each deal damage equal to their
 *  power to target creature an opponent controls."
 *
 * Verifies the 1-or-2 your-creature target group gets tapped and each deals damage equal to its
 * own power to the single opponent's creature; and the single-creature mode. Targets are supplied
 * at cast time in requirement order: the opponent's creature, then the clobberers.
 */
class CoordinatedClobberingScenarioTest : ScenarioTestBase() {

    init {
        context("Coordinated Clobbering") {

            test("two clobberers tap and each deals its power to the opponent's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Coordinated Clobbering")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withCardOnBattlefield(2, "Air Elemental") // 4/4 victim
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val victim = game.findPermanent("Air Elemental")!!
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Coordinated Clobbering"
                }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(
                            ChosenTarget.Permanent(victim),
                            ChosenTarget.Permanent(giant),
                            ChosenTarget.Permanent(bears),
                        ),
                    ),
                )
                withClue("Casting Coordinated Clobbering should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Both clobberers are tapped") {
                    (game.state.getEntity(giant)?.get<TappedComponent>() == TappedComponent) shouldBe true
                    (game.state.getEntity(bears)?.get<TappedComponent>() == TappedComponent) shouldBe true
                }
                withClue("3 + 2 = 5 damage kills the 4/4 Air Elemental") {
                    game.isOnBattlefield("Air Elemental") shouldBe false
                    game.isInGraveyard(2, "Air Elemental") shouldBe true
                }
            }

            test("single clobberer taps and deals its power to the opponent's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Coordinated Clobbering")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 victim
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val victim = game.findPermanents("Grizzly Bears").first {
                    game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId == game.player2Id
                }
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Coordinated Clobbering"
                }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(
                            ChosenTarget.Permanent(victim),
                            ChosenTarget.Permanent(giant),
                        ),
                    ),
                )
                cast.error shouldBe null
                game.resolveStack()

                withClue("The single clobberer is tapped") {
                    (game.state.getEntity(giant)?.get<TappedComponent>() == TappedComponent) shouldBe true
                }
                withClue("3 damage kills the 2/2 victim") {
                    val victimZone = game.state.getZone(com.wingedsheep.engine.state.ZoneKey(game.player2Id, Zone.GRAVEYARD))
                    (victim in victimZone) shouldBe true
                }
            }

            test("uses power granted by an attached aura") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Coordinated Clobbering")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Overgrown Zealot") // 0/4
                    .withCardAttachedTo(1, "Sheltered by Ghosts", "Overgrown Zealot") // now 1/4
                    .withCardOnBattlefield(2, "Veteran Survivor") // 2/1 victim
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zealot = game.findPermanent("Overgrown Zealot")!!
                val victim = game.findPermanent("Veteran Survivor")!!
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Coordinated Clobbering"
                }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(
                            ChosenTarget.Permanent(victim),
                            ChosenTarget.Permanent(zealot),
                        ),
                    ),
                )
                cast.error shouldBe null
                game.resolveStack()

                withClue("The aura's +1/+0 makes the Zealot deal 1 damage and kill the 2/1") {
                    game.isOnBattlefield("Veteran Survivor") shouldBe false
                    game.isInGraveyard(2, "Veteran Survivor") shouldBe true
                }
            }
        }
    }
}
