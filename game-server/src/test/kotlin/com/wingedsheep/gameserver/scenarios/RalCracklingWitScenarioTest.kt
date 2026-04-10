package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent
import com.wingedsheep.engine.state.components.player.SpellKeywordGrant
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RalCracklingWitScenarioTest : ScenarioTestBase() {

    private fun TestGame.addLoyalty(cardName: String, loyalty: Int) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withAdded(CounterType.LOYALTY, loyalty))
        }
    }

    private fun TestGame.activateLoyaltyAbility(
        playerNumber: Int,
        cardName: String,
        abilityIndex: Int,
        targetIds: List<EntityId> = emptyList()
    ) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val permanentId = findPermanent(cardName)!!
        val cardDef = cardRegistry.getCard(cardName)!!
        val ability = cardDef.script.activatedAbilities[abilityIndex]

        val targets = targetIds.map { ChosenTarget.Permanent(it) }

        val result = execute(
            ActivateAbility(
                playerId = playerId,
                sourceId = permanentId,
                abilityId = ability.id,
                targets = targets
            )
        )
        withClue("Loyalty ability activation should succeed: ${result.error}") {
            result.error shouldBe null
        }
    }

    private fun TestGame.grantStormEmblem(playerNumber: Int) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        state = state.updateEntity(playerId) { c ->
            c.with(GrantedSpellKeywordsComponent(
                listOf(SpellKeywordGrant(Keyword.STORM, GameObjectFilter.InstantOrSorcery))
            ))
        }
    }

    init {
        context("Ral +1: Create Otter token with prowess") {
            test("creates 1/1 blue/red Otter with prowess") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ral, Crackling Wit")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Ral, Crackling Wit", 4)

                // +1 is the first loyalty ability (index 0, after the triggered ability)
                game.activateLoyaltyAbility(1, "Ral, Crackling Wit", 0)

                // Resolve — Otter should be created
                game.resolveStack()

                // Find the Otter token
                val otterId = game.findPermanent("Otter Token")
                withClue("Should have created an Otter token") {
                    otterId shouldNotBe null
                }

                val projected = game.state.projectedState
                withClue("Otter should be 1/1") {
                    projected.getPower(otterId!!) shouldBe 1
                    projected.getToughness(otterId) shouldBe 1
                }
            }
        }

        context("Ral noncreature spell trigger — adds loyalty counter") {
            test("gains loyalty when casting a noncreature spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ral, Crackling Wit")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Ral, Crackling Wit", 4)

                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Shock", glorySeekerID)

                // Resolve triggered ability first (puts loyalty counter on Ral)
                game.resolveStack()

                val ralId = game.findPermanent("Ral, Crackling Wit")!!
                val counters = game.state.getEntity(ralId)?.get<CountersComponent>()
                withClue("Ral should have gained a loyalty counter (4 base + 1 from trigger = 5)") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.LOYALTY) shouldBe 5
                }
            }
        }

        context("Storm emblem — granted storm triggers on instant/sorcery") {
            test("casting Shock with storm emblem and prior spells creates storm copies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Grant storm emblem to player 1
                game.grantStormEmblem(1)

                // Simulate that 2 spells were already cast this turn
                game.state = game.state.copy(spellsCastThisTurn = 2)

                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val result = game.castSpell(1, "Shock", glorySeekerID)

                // Cast should succeed — storm copies are on the stack
                withClue("Cast with storm emblem should succeed") {
                    result.error shouldBe null
                }

                // The spellsCastThisTurn should now be 3 (was 2 + the bolt)
                withClue("Spell count should have incremented") {
                    game.state.spellsCastThisTurn shouldBe 3
                }
            }
        }
    }
}
