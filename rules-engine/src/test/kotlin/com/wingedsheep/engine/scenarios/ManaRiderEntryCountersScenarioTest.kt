package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import io.kotest.matchers.shouldBe

/**
 * [ManaSpellRider.EntersWithCountersWhenSpent] — "If that mana is spent on a multicolored creature
 * spell, that creature enters with an additional +1/+1 counter on it" (Guildmages' Forum).
 *
 * Not a triggered ability: nothing goes on the stack above the spell, and the counter is owed to the
 * spell at payment time, then placed as the permanent enters. The filter is matched against the cast
 * spell, so mana spent on a monocolored creature or a multicolored noncreature permanent does nothing, and each
 * rider-carrying mana spent owes its own counter. Riders reach the spell whether the mana floated in
 * the pool or the auto-payer tapped the source.
 */
class ManaRiderEntryCountersScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(RiderGrove, RiderMeadow, GoldBear, GreenBear, GoldShrine))

        fun board(vararg hand: String, plainWhite: Boolean = false) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Rider Grove")
            .withCardOnBattlefield(1, if (plainWhite) "Plains" else "Rider Meadow")
            .apply { hand.forEach { withCardInHand(1, it) } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.tap(name: String, def: CardDefinition) {
            val id = findPermanent(name)!!
            execute(ActivateAbility(player1Id, id, def.activatedAbilities.single().id)).error shouldBe null
        }

        fun TestGame.plusOnes(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("a multicolored creature paid with rider mana enters with a counter, and nothing goes on the stack") {
            val game = board("Gold Bear")
            game.tap("Rider Grove", RiderGrove)
            game.tap("Rider Meadow", RiderMeadow)

            game.castSpell(1, "Gold Bear").error shouldBe null
            game.state.stack.size shouldBe 1

            game.resolveStack()
            // Two rider-carrying mana were spent on it: one counter each.
            game.plusOnes(game.findPermanent("Gold Bear")!!) shouldBe 2
        }

        test("one rider mana among the payment owes one counter") {
            val game = board("Gold Bear", plainWhite = true)
            game.tap("Rider Grove", RiderGrove)
            game.castSpell(1, "Gold Bear").error shouldBe null
            game.resolveStack()
            game.plusOnes(game.findPermanent("Gold Bear")!!) shouldBe 1
        }

        test("riders reach the spell when the auto-payer taps the sources") {
            val game = board("Gold Bear")
            game.castSpell(1, "Gold Bear").error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.plusOnes(game.findPermanent("Gold Bear")!!) shouldBe 2
        }

        test("mana spent on a monocolored creature spell does nothing") {
            val game = board("Green Bear")
            game.tap("Rider Grove", RiderGrove)
            game.castSpell(1, "Green Bear").error shouldBe null
            game.resolveStack()
            game.plusOnes(game.findPermanent("Green Bear")!!) shouldBe 0
        }

        test("mana spent on a multicolored noncreature permanent spell does nothing") {
            val game = board("Gold Shrine")
            game.tap("Rider Grove", RiderGrove)
            game.tap("Rider Meadow", RiderMeadow)
            game.castSpell(1, "Gold Shrine").error shouldBe null
            game.resolveStack()
            game.plusOnes(game.findPermanent("Gold Shrine")!!) shouldBe 0
        }
    }

    companion object {
        private val multicoloredCreature = GameObjectFilter.Creature and GameObjectFilter.Multicolored

        private fun riderLand(name: String, color: Color) = card(name) {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(
                    color,
                    riders = setOf(ManaSpellRider.EntersWithCountersWhenSpent(multicoloredCreature)),
                )
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }

        private val RiderGrove = riderLand("Rider Grove", Color.GREEN)
        private val RiderMeadow = riderLand("Rider Meadow", Color.WHITE)

        private val GoldBear = CardDefinition.creature(
            name = "Gold Bear",
            manaCost = ManaCost.parse("{G}{W}"),
            subtypes = emptySet(),
            power = 2,
            toughness = 2,
        )

        private val GreenBear = CardDefinition.creature(
            name = "Green Bear",
            manaCost = ManaCost.parse("{G}"),
            subtypes = emptySet(),
            power = 2,
            toughness = 2,
        )

        private val GoldShrine = card("Gold Shrine") {
            manaCost = "{G}{W}"
            typeLine = "Enchantment"
        }
    }
}
