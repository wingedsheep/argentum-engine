package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.grn.cards.GuildmagesForum
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Guildmages' Forum (GRN #250).
 *
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color. If that mana is spent on a multicolored creature spell, that
 * creature enters with an additional +1/+1 counter on it.
 *
 * The counter isn't a trigger: nothing goes on the stack, and it's placed as the creature enters, so
 * Hardened Scales adds one more. The {C} ability carries no rider, and a monocolored creature gets nothing.
 */
class GuildmagesForumScenarioTest : ScenarioTestBase() {

    init {
        val colorlessAbility = GuildmagesForum.activatedAbilities[0].id
        val anyColorAbility = GuildmagesForum.activatedAbilities[1].id

        fun board(creature: String, scales: Boolean = false) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Guildmages' Forum")
            .withLandsOnBattlefield(1, "Forest", 2)
            .apply { if (scales) withCardOnBattlefield(1, "Hardened Scales") }
            .withCardInHand(1, creature)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.plusOnes(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun TestGame.makeWhite() {
            val forum = findPermanent("Guildmages' Forum")!!
            execute(ActivateAbility(player1Id, forum, anyColorAbility, manaColorChoice = Color.WHITE)).error shouldBe null
        }

        test("its colored mana spent on a multicolored creature spell gives an additional +1/+1 counter") {
            val game = board("Watchwolf")
            game.makeWhite()
            game.castSpell(1, "Watchwolf").error shouldBe null
            game.state.stack.size shouldBe 1

            game.resolveStack()
            game.plusOnes(game.findPermanent("Watchwolf")!!) shouldBe 1
        }

        test("the counter is placed as the creature enters, so Hardened Scales adds one more") {
            val game = board("Watchwolf", scales = true)
            game.makeWhite()
            game.castSpell(1, "Watchwolf").error shouldBe null
            game.resolveStack()
            game.plusOnes(game.findPermanent("Watchwolf")!!) shouldBe 2
        }

        test("spent on a monocolored creature spell it gives nothing") {
            val game = board("Grizzly Bears")
            val forum = game.findPermanent("Guildmages' Forum")!!
            game.execute(ActivateAbility(game.player1Id, forum, anyColorAbility, manaColorChoice = Color.GREEN)).error shouldBe null
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 0
        }

        test("its {C} carries no rider") {
            val game = board("Grizzly Bears")
            val forum = game.findPermanent("Guildmages' Forum")!!
            game.execute(ActivateAbility(game.player1Id, forum, colorlessAbility)).error shouldBe null
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 0
        }
    }
}
