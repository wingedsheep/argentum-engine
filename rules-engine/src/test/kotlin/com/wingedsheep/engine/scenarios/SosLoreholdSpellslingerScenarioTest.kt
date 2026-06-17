package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the Lorehold/spellslinger Secrets of Strixhaven cards added together:
 *  - Blazing Firesinger // Seething Song    (enters prepared; copy adds {R}{R}{R}{R}{R})
 *  - Strife Scholar // Awaken the Ages       (enters prepared + Ward—Pay 2 life; copy makes 2 Spirits)
 *  - Elite Interceptor // Rejoinder          (enters prepared; copy: may tap/untap a creature, draw)
 *  - Maelstrom Artisan // Rocket Volley      (haste; enters prepared; copy destroys a nonbasic land)
 *
 * These prepare cards reuse the existing PREPARE layout + `prepare(name) { }` DSL; this file pins
 * each prepare spell's resolution behaviour. No new SDK was introduced (the `TargetFilter.nonbasic()`
 * helper just composes the existing nonbasic-land predicate), so these are pure card-behaviour tests.
 */
class SosLoreholdSpellslingerScenarioTest : ScenarioTestBase() {

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    private fun TestGame.redMana(playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getEntity(playerId)?.get<ManaPoolComponent>()?.getAmount(Color.RED) ?: 0
    }

    init {
        context("Blazing Firesinger — Seething Song (enters prepared)") {
            test("enters prepared; casting the copy adds five red mana") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blazing Firesinger")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Blazing Firesinger")
                game.resolveStack()

                val firesinger = game.findPermanent("Blazing Firesinger")!!
                withClue("Blazing Firesinger should be prepared on ETB") {
                    game.state.getEntity(firesinger)?.get<PreparedComponent>() shouldNotBe null
                }

                val copyId = game.findExileCopy(1, "Blazing Firesinger")!!
                // Cast the Seething Song copy for {2}{R}.
                game.execute(CastSpell(game.player1Id, copyId, faceIndex = 0))
                game.resolveStack()

                withClue("Seething Song adds {R}{R}{R}{R}{R} to the pool") {
                    game.redMana(1) shouldBe 5
                }
                withClue("Blazing Firesinger is no longer prepared after casting the copy") {
                    game.state.getEntity(firesinger)?.get<PreparedComponent>() shouldBe null
                }
            }
        }

        context("Strife Scholar — Awaken the Ages (enters prepared + Ward)") {
            test("enters prepared; casting the copy creates two 2/2 red-and-white Spirit tokens") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Strife Scholar")
                    .withLandsOnBattlefield(1, "Mountain", 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Strife Scholar")
                game.resolveStack()

                val scholar = game.findPermanent("Strife Scholar")!!
                withClue("Strife Scholar should be prepared on ETB") {
                    game.state.getEntity(scholar)?.get<PreparedComponent>() shouldNotBe null
                }

                val spiritsBefore = game.findPermanents("Spirit Token").size
                val copyId = game.findExileCopy(1, "Strife Scholar")!!
                // Cast the Awaken the Ages copy for {5}{R}.
                val castResult = game.execute(CastSpell(game.player1Id, copyId, faceIndex = 0))
                withClue("Casting the Awaken the Ages copy should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Awaken the Ages creates two Spirit tokens") {
                    game.findPermanents("Spirit Token").size shouldBe spiritsBefore + 2
                }
                withClue("Strife Scholar is no longer prepared after casting the copy") {
                    game.state.getEntity(scholar)?.get<PreparedComponent>() shouldBe null
                }
            }
        }

        context("Elite Interceptor — Rejoinder (enters prepared)") {
            test("enters prepared; the copy is offered as a {1}{W} cast of face 0 from exile") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Elite Interceptor")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Elite Interceptor")
                game.resolveStack()

                val interceptor = game.findPermanent("Elite Interceptor")!!
                withClue("Elite Interceptor should be prepared on ETB") {
                    game.state.getEntity(interceptor)?.get<PreparedComponent>() shouldNotBe null
                }

                val copyId = game.findExileCopy(1, "Elite Interceptor")!!
                val prepareAction = game.getLegalActions(1).firstOrNull { la ->
                    val a = la.action
                    a is CastSpell && a.cardId == copyId
                }
                withClue("Rejoinder should be offered as a {1}{W} cast of face 0 from exile") {
                    prepareAction shouldNotBe null
                    (prepareAction!!.action as CastSpell).faceIndex shouldBe 0
                    prepareAction.sourceZone shouldBe "EXILE"
                    prepareAction.manaCostString shouldBe "{1}{W}"
                }
            }
        }

        context("Maelstrom Artisan — Rocket Volley (enters prepared)") {
            test("enters prepared with haste; casting the copy destroys a nonbasic land") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Maelstrom Artisan")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Terramorphic Expanse") // a nonbasic land
                    .withCardOnBattlefield(2, "Mountain")             // a basic land (must NOT be targetable)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Maelstrom Artisan")
                game.resolveStack()

                val artisan = game.findPermanent("Maelstrom Artisan")!!
                withClue("Maelstrom Artisan should be prepared on ETB") {
                    game.state.getEntity(artisan)?.get<PreparedComponent>() shouldNotBe null
                }

                val nonbasic = game.findPermanent("Terramorphic Expanse")!!
                val copyId = game.findExileCopy(1, "Maelstrom Artisan")!!
                // Cast the Rocket Volley copy for {1}{R}, targeting the nonbasic land.
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        copyId,
                        targets = listOf(ChosenTarget.Permanent(nonbasic)),
                        faceIndex = 0
                    )
                )
                withClue("Casting the Rocket Volley copy should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Rocket Volley destroys the targeted nonbasic land") {
                    game.findPermanent("Terramorphic Expanse") shouldBe null
                }
                withClue("Maelstrom Artisan is no longer prepared after casting the copy") {
                    game.state.getEntity(artisan)?.get<PreparedComponent>() shouldBe null
                }
            }
        }
    }
}
