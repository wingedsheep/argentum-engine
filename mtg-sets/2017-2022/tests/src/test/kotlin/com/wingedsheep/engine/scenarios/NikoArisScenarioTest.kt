package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.NikoAris
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Niko Aris (KHM #225).
 *
 * - The enters trigger creates as many Shards as the X Niko was cast with.
 * - The +1 makes a creature unblockable and sets up a delayed trigger that bounces it to its
 *   owner's hand once it deals damage.
 * - The first −1 deals 2 damage per card drawn this turn, all at once, to a tapped creature.
 */
class NikoArisScenarioTest : ScenarioTestBase() {

    private val plusOne = NikoAris.activatedAbilities[0].id
    private val damageMinusOne = NikoAris.activatedAbilities[1].id

    private fun TestGame.loyalty(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        context("Niko Aris") {

            test("enters with X = 2 and creates two Shard tokens") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Niko Aris")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Niko Aris", xValue = 2).error shouldBe null
                game.resolveStack()

                game.findPermanent("Niko Aris") shouldNotBe null
                game.findPermanents("Shard").size shouldBe 2
            }

            test("+1: the creature can't be blocked and returns to its owner's hand after dealing damage") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Niko Aris")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val niko = game.findPermanent("Niko Aris")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = niko,
                        abilityId = plusOne,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()
                game.loyalty(niko) shouldBe 4

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("the targeted creature can't be blocked") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
                }
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("combat damage dealt, then the delayed trigger bounced the Bears") {
                    game.getLifeTotal(2) shouldBe 18
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("+1 with no target still adds loyalty") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Niko Aris")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val niko = game.findPermanent("Niko Aris")!!

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = niko, abilityId = plusOne)
                ).error shouldBe null
                game.resolveStack()
                game.loyalty(niko) shouldBe 4
            }

            test("−1 deals 2 damage per card drawn this turn to a tapped creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Niko Aris")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withCardsDrawnThisTurn(1, 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val niko = game.findPermanent("Niko Aris")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = niko,
                        abilityId = damageMinusOne,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.loyalty(niko) shouldBe 2
                withClue("one card drawn means 2 damage: the 3/3 Giant survives") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
                }
            }
        }
    }
}
