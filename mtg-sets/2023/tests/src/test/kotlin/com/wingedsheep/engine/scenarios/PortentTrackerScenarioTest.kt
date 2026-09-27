package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.ProtectorComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.PortentTracker
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Portent Tracker (MOM #201) — "{T}: Untap target land." and "{T}: Choose target battle. If an
 * opponent protects it, remove a defense counter from it. Otherwise, put a defense counter on it.
 * Activate only as a sorcery."
 *
 * The battle branch keys on the battle's *protector* (CR 310.8), not its controller: a Siege you
 * cast is yours but protected by an opponent, so it loses a counter; a Siege the opponent cast is
 * protected by you, so it gains one.
 */
class PortentTrackerScenarioTest : ScenarioTestBase() {

    private val untapAbility = PortentTracker.activatedAbilities[0].id
    private val battleAbility = PortentTracker.activatedAbilities[1].id

    private fun defenseOf(game: TestGame, battle: EntityId): Int =
        game.state.getEntity(battle)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) ?: 0

    private fun protectorOf(game: TestGame, battle: EntityId): EntityId? =
        game.state.getEntity(battle)?.get<ProtectorComponent>()?.playerId

    private fun activateOnBattle(game: TestGame, battle: EntityId) =
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Portent Tracker")!!,
                abilityId = battleAbility,
                targets = listOf(ChosenTarget.Permanent(battle))
            )
        )

    init {
        context("{T}: choose target battle") {

            test("a battle an opponent protects (your own Siege) loses a defense counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Portent Tracker")
                    .withCardOnBattlefield(1, "Invasion of Innistrad")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.checkStateBasedActions()

                val battle = game.findPermanent("Invasion of Innistrad")!!
                withClue("a Siege is protected by an opponent of its controller (CR 310.12a)") {
                    protectorOf(game, battle) shouldBe game.player2Id
                }
                defenseOf(game, battle) shouldBe 5

                activateOnBattle(game, battle).error shouldBe null
                game.resolveStack()

                withClue("opponent protects it -> remove a defense counter") {
                    defenseOf(game, battle) shouldBe 4
                }
            }

            test("a battle you protect (the opponent's Siege) gains a defense counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Portent Tracker")
                    .withCardOnBattlefield(2, "Invasion of Innistrad")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.checkStateBasedActions()

                val battle = game.findPermanent("Invasion of Innistrad")!!
                withClue("the opponent's Siege is protected by you") {
                    protectorOf(game, battle) shouldBe game.player1Id
                }

                activateOnBattle(game, battle).error shouldBe null
                game.resolveStack()

                withClue("otherwise -> put a defense counter on it") {
                    defenseOf(game, battle) shouldBe 6
                }
            }

            test("it can only be activated as a sorcery") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Portent Tracker")
                    .withCardOnBattlefield(1, "Invasion of Innistrad")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.checkStateBasedActions()
                // Hand priority to player 1 on the opponent's turn.
                game.passPriority()

                val battle = game.findPermanent("Invasion of Innistrad")!!
                activateOnBattle(game, battle).error shouldNotBe null
                defenseOf(game, battle) shouldBe 5
            }
        }

        context("{T}: untap target land") {

            test("untaps a tapped land") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Portent Tracker")
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                val tracker = game.findPermanent("Portent Tracker")!!
                game.state.getEntity(forest)
                    ?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tracker,
                        abilityId = untapAbility,
                        targets = listOf(ChosenTarget.Permanent(forest))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the land is untapped and the Tracker paid {T}") {
                    game.state.getEntity(forest)
                        ?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe false
                    game.state.getEntity(tracker)
                        ?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
                }
            }
        }
    }
}
