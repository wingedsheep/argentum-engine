package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

class AlchemistsTalentTest : ScenarioTestBase() {

    init {
        context("Alchemist's Talent Level 1 — ETB creates two tapped Treasure tokens") {
            test("two tapped Treasures appear on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Alchemist's Talent")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Alchemist's Talent")
                cast.error shouldBe null
                game.resolveStack()

                val treasuresControlled = game.state.getBattlefield()
                    .mapNotNull { game.state.getEntity(it) }
                    .count { entity ->
                        val card = entity.get<CardComponent>()
                        card?.name == "Treasure"
                    }
                withClue("Two Treasure tokens should be on the battlefield") {
                    treasuresControlled shouldBe 2
                }
            }
        }

        context("Alchemist's Talent Level 2 — grants Treasures a second activated ability") {
            test("Treasure you control surfaces the granted '{T}, Sacrifice: Add two mana' ability in legal actions") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Alchemist's Talent", classLevel = 2)
                    .withCardOnBattlefield(1, "Treasure", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val session = GameSession(cardRegistry = cardRegistry)
                val ws1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val ws2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                session.injectStateForTesting(
                    game.state,
                    mapOf(
                        game.player1Id to PlayerSession(ws1, game.player1Id, "Player"),
                        game.player2Id to PlayerSession(ws2, game.player2Id, "Opponent")
                    )
                )

                val treasureId = game.findPermanent("Treasure")!!
                val cardDef = cardRegistry.getCard("Alchemist's Talent")!!
                // Level 2's granted ability is the activated ability declared inside the
                // GrantActivatedAbility static ability at classLevel 2.
                val grantedAbility = cardDef.script.classLevels
                    .first { it.level == 2 }
                    .staticAbilities
                    .filterIsInstance<com.wingedsheep.sdk.scripting.GrantActivatedAbility>()
                    .single()
                    .ability

                val legalActions = session.getLegalActions(game.player1Id)

                val grantedActivations = legalActions.filter { info ->
                    val act = info.action
                    act is ActivateAbility &&
                        act.sourceId == treasureId &&
                        act.abilityId == grantedAbility.id
                }

                withClue(
                    "Treasure should expose Alchemist's Talent level 2's granted ability " +
                        "in legal actions (got ${legalActions.size} actions: " +
                        "${legalActions.map { it.description }})"
                ) {
                    grantedActivations.shouldHaveSize(1)
                }
            }

            test("granted ability is NOT present when Alchemist's Talent is still at level 1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Alchemist's Talent", classLevel = 1)
                    .withCardOnBattlefield(1, "Treasure", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val session = GameSession(cardRegistry = cardRegistry)
                val ws1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val ws2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                session.injectStateForTesting(
                    game.state,
                    mapOf(
                        game.player1Id to PlayerSession(ws1, game.player1Id, "Player"),
                        game.player2Id to PlayerSession(ws2, game.player2Id, "Opponent")
                    )
                )

                val treasureId = game.findPermanent("Treasure")!!
                val cardDef = cardRegistry.getCard("Alchemist's Talent")!!
                val grantedAbility = cardDef.script.classLevels
                    .first { it.level == 2 }
                    .staticAbilities
                    .filterIsInstance<com.wingedsheep.sdk.scripting.GrantActivatedAbility>()
                    .single()
                    .ability

                val legalActions = session.getLegalActions(game.player1Id)

                val grantedActivations = legalActions.filter { info ->
                    val act = info.action
                    act is ActivateAbility &&
                        act.sourceId == treasureId &&
                        act.abilityId == grantedAbility.id
                }

                withClue("Level 1 Talent must not yet grant the level-2 ability to Treasures") {
                    grantedActivations.shouldHaveSize(0)
                }
            }

            test("activating the granted ability sacrifices the Treasure and adds two mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Alchemist's Talent", classLevel = 2)
                    .withCardOnBattlefield(1, "Treasure", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val treasureId = game.findPermanent("Treasure")!!
                val cardDef = cardRegistry.getCard("Alchemist's Talent")!!
                val grantedAbility = cardDef.script.classLevels
                    .first { it.level == 2 }
                    .staticAbilities
                    .filterIsInstance<com.wingedsheep.sdk.scripting.GrantActivatedAbility>()
                    .single()
                    .ability

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = treasureId,
                        abilityId = grantedAbility.id,
                        manaColorChoice = com.wingedsheep.sdk.core.Color.RED
                    )
                )
                withClue("Granted Treasure ability should activate: ${result.error}") {
                    result.error shouldBe null
                }

                // Mana abilities resolve immediately (no stack). Treasure should be
                // sacrificed, treasureMana counter should be at 2, and red pool at 2.
                val poolAfter = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                poolAfter.shouldNotBeNull()
                withClue("Two red mana should have been added") { poolAfter.red shouldBe 2 }
                withClue("Both units should be tagged as Treasure mana") {
                    poolAfter.treasureMana shouldBe 2
                }
                withClue("Treasure should no longer be on the battlefield (sacrificed)") {
                    game.findPermanent("Treasure") shouldBe null
                }
            }
        }

        context("Alchemist's Talent Level 3 — damages each opponent when paid with Treasure mana") {
            test("casting a spell with treasure-tagged mana deals spell's mana value to each opponent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Alchemist's Talent", classLevel = 3)
                    .withCardInHand(1, "Scorching Spear")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Seed the active player's mana pool with one red mana that is
                // tagged as Treasure mana, simulating having just sacrificed a
                // Treasure and chosen red. The cast will spend this mana from
                // the pool and the engine will flag the cast as
                // `paidWithTreasureMana = true`, firing Alchemist's Talent's
                // level 3 trigger.
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(
                        ManaPoolComponent(red = 1, treasureMana = 1)
                    )
                }

                val opponentLifeBefore = game.getLifeTotal(2)

                val cast = game.castSpellTargetingPlayer(1, "Scorching Spear", 2)
                withClue("Scorching Spear should cast successfully: ${cast.error}") {
                    cast.error shouldBe null
                }

                // Resolve the level 3 trigger and the Scorching Spear itself.
                game.resolveStack()

                val opponentLifeAfter = game.getLifeTotal(2)
                // Scorching Spear's mana value is 1, so the level 3 trigger deals 1
                // to each opponent. Scorching Spear then deals 1 to the targeted
                // opponent. Total damage to opponent: 1 (trigger) + 1 (spear) = 2.
                withClue("Opponent should take 2 damage (1 from Alchemist's Talent trigger + 1 from Scorching Spear)") {
                    (opponentLifeBefore - opponentLifeAfter) shouldBe 2
                }
            }

            test("casting without treasure mana does NOT trigger Alchemist's Talent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Alchemist's Talent", classLevel = 3)
                    .withCardInHand(1, "Scorching Spear")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentLifeBefore = game.getLifeTotal(2)

                val cast = game.castSpellTargetingPlayer(1, "Scorching Spear", 2)
                cast.error shouldBe null
                game.resolveStack()

                val opponentLifeAfter = game.getLifeTotal(2)
                withClue("Opponent should only take Scorching Spear's 1 damage — no treasure-trigger damage") {
                    (opponentLifeBefore - opponentLifeAfter) shouldBe 1
                }
            }
        }
    }
}
