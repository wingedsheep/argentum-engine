package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.znr.cards.ThroneOfMakindi
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Throne of Makindi (ZNR #265) — Land.
 *
 *   {T}: Add {C}.
 *   {1}, {T}: Put a charge counter on this land.
 *   {T}, Remove a charge counter from this land: Add two mana of any one color. Spend this mana
 *   only to cast kicked spells.
 */
class ThroneOfMakindiScenarioTest : ScenarioTestBase() {

    private val chargeAbility = ThroneOfMakindi.activatedAbilities[1].id
    private val kickedManaAbility = ThroneOfMakindi.activatedAbilities[2].id

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Throne of Makindi")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.chargeCounters(throne: EntityId): Int =
        state.getEntity(throne)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    init {
        test("{1}, {T} puts a charge counter on the Throne") {
            val game = board()
            val throne = game.findPermanent("Throne of Makindi")!!

            game.execute(ActivateAbility(game.player1Id, throne, chargeAbility)).error shouldBe null
            game.resolveStack()

            game.chargeCounters(throne) shouldBe 1
            game.state.getEntity(throne)!!.has<TappedComponent>() shouldBe true
        }

        test("the kicked-spell mana ability needs a charge counter to remove") {
            val game = board()
            val throne = game.findPermanent("Throne of Makindi")!!

            game.execute(ActivateAbility(game.player1Id, throne, kickedManaAbility)).error shouldNotBe null
            game.state.getEntity(throne)!!.has<TappedComponent>() shouldBe false
        }

        test("removing a charge counter adds two mana of one chosen color, restricted to kicked spells") {
            val game = board()
            val throne = game.findPermanent("Throne of Makindi")!!
            game.state = game.state.updateEntity(throne) {
                it.with(CountersComponent().withAdded(CounterType.CHARGE, 1))
            }

            game.execute(
                ActivateAbility(game.player1Id, throne, kickedManaAbility, manaColorChoice = Color.RED)
            ).error shouldBe null

            game.chargeCounters(throne) shouldBe 0
            val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
            withClue("two restricted mana of the chosen color") {
                pool.restrictedMana shouldHaveSize 2
                pool.restrictedMana.forEach { entry ->
                    entry.color shouldBe Color.RED
                    entry.restriction shouldBe ManaRestriction.KickedSpellsOnly
                }
            }
        }
    }
}
