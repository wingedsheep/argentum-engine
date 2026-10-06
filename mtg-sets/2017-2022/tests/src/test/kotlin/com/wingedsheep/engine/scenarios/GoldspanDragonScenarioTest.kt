package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.GoldspanDragon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Goldspan Dragon (KHM) — "Whenever this creature attacks or becomes the target of a spell, create a
 * Treasure token. / Treasures you control have '{T}, Sacrifice this artifact: Add two mana of any one
 * color.'"
 *
 * Pins the two halves of the `Triggers.or(attacks, becomesTarget(spellsOnly))` disjunction and the
 * granted tap-and-sacrifice mana ability resolving against the Treasure it was granted to.
 */
class GoldspanDragonScenarioTest : ScenarioTestBase() {

    private val grantedAbilityId =
        GoldspanDragon.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.id

    init {
        test("attacking creates a Treasure") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Goldspan Dragon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Goldspan Dragon" to 2)).error shouldBe null
            game.resolveStack()

            withClue("the attack trigger made one Treasure") {
                game.findPermanents("Treasure").size shouldBe 1
            }
        }

        test("becoming the target of a spell creates a Treasure; the spell resolves after it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Goldspan Dragon")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dragon = game.findPermanent("Goldspan Dragon")!!
            game.castSpell(2, "Shock", dragon).error shouldBe null
            game.resolveStack()

            withClue("its controller got the Treasure, and the 4/4 survives Shock") {
                game.findPermanents("Treasure").size shouldBe 1
                game.isOnBattlefield("Goldspan Dragon") shouldBe true
            }
        }

        test("a Treasure taps and sacrifices for two mana of one color") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Goldspan Dragon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Goldspan Dragon" to 2)).error shouldBe null
            game.resolveStack()
            val treasure = game.findPermanent("Treasure")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = treasure,
                    abilityId = grantedAbilityId,
                    manaColorChoice = Color.RED,
                )
            ).error shouldBe null

            withClue("two red mana, and the Treasure is gone") {
                val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
                pool.red shouldBe 2
                pool.total shouldBe 2
                game.findPermanent("Treasure") shouldBe null
            }
        }
    }
}
