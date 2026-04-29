package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Bre of Clan Stoutarm.
 *
 * {2}{R}{W} Legendary Creature — Giant Warrior 4/4
 *
 * {1}{W}, {T}: Another target creature you control gains flying and lifelink until end of turn.
 * At the beginning of your end step, if you gained life this turn, exile cards from the top of
 * your library until you exile a nonland card. You may cast that card without paying its mana
 * cost if the spell's mana value is less than or equal to the amount of life you gained this
 * turn. Otherwise, put it into your hand.
 */
class BreOfClanStoutarmScenarioTest : ScenarioTestBase() {

    init {
        context("Bre of Clan Stoutarm end-step trigger") {

            test("does not trigger when no life was gained this turn") {
                val game = scenario()
                    .withPlayers("Active", "Opponent")
                    .withCardOnBattlefield(1, "Bre of Clan Stoutarm")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Shock")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)

                // No exile entry — trigger never fired (intervening-if condition false).
                game.state.getExile(game.player1Id).size shouldBe 0
                // Library still untouched.
                game.state.getLibrary(game.player1Id).size shouldBe 3
            }

            test("exiles cards and grants free cast when nonland mana value <= life gained") {
                val game = scenario()
                    .withPlayers("Active", "Opponent")
                    .withCardOnBattlefield(1, "Bre of Clan Stoutarm")
                    .withCardInHand(1, "Recuperate")  // {3}{W} — gain 5 life
                    .withLandsOnBattlefield(1, "Plains", 4)
                    // Library top→bottom: Mountain (land, exiled first), Shock (nonland, exiled
                    // and granted free cast), Forest (untouched).
                    .withCardInLibrary(1, "Mountain")  // top (land — exiled)
                    .withCardInLibrary(1, "Shock")     // nonland — exiled with free-cast
                    .withCardInLibrary(1, "Forest")    // bottom (untouched)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Recuperate targeting self → gain 5 life this turn.
                game.castSpellTargetingPlayer(1, "Recuperate", targetPlayerNumber = 1)
                game.resolveStack()

                // Confirm the LIFE_GAINED tracker accumulated.
                val gainedAmount = game.state.getEntity(game.player1Id)
                    ?.get<LifeGainedAmountThisTurnComponent>()?.amount
                gainedAmount shouldBe 5

                // Advance to the end step — Bre's triggered ability fires and resolves.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                // Mountain (land) and Shock (nonland) are now in exile.
                val exile = game.state.getExile(game.player1Id)
                val mountainExiled = exile.firstOrNull { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Mountain"
                }
                val shockExiled = exile.firstOrNull { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Shock"
                }
                mountainExiled shouldNotBe null
                shockExiled shouldNotBe null
                exile.size shouldBe 2

                // Forest is untouched at the bottom of the library.
                val library = game.state.getLibrary(game.player1Id)
                library.size shouldBe 1
                game.state.getEntity(library.first())?.get<CardComponent>()?.name shouldBe "Forest"

                // Shock has free-cast permission — Shock MV=1 <= 5 life gained.
                val shockContainer = game.state.getEntity(shockExiled!!)!!
                shockContainer.get<MayPlayFromExileComponent>() shouldNotBe null
                shockContainer.get<PlayWithoutPayingCostComponent>() shouldNotBe null
            }

            test("puts nonland into hand when mana value > life gained") {
                val game = scenario()
                    .withPlayers("Active", "Opponent")
                    .withCardOnBattlefield(1, "Bre of Clan Stoutarm")
                    .withCardInHand(1, "Recuperate")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    // Aurora Awakener {6}{G} = mana value 7, above the 5 life we will gain.
                    // Library top→bottom: Mountain (land), Aurora Awakener (nonland), Forest.
                    .withCardInLibrary(1, "Mountain")  // top (land — exiled)
                    .withCardInLibrary(1, "Aurora Awakener")  // nonland — goes to hand
                    .withCardInLibrary(1, "Forest")  // bottom (untouched)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Recuperate", targetPlayerNumber = 1)
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                // Mountain (land) is exiled; Aurora Awakener is in hand instead.
                val exile = game.state.getExile(game.player1Id)
                val mountainExiled = exile.firstOrNull { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Mountain"
                }
                mountainExiled shouldNotBe null
                exile.size shouldBe 1

                val hand = game.state.getHand(game.player1Id)
                val auroraInHand = hand.firstOrNull { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Aurora Awakener"
                }
                auroraInHand shouldNotBe null
            }
        }
    }
}
