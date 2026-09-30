package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ConvertEmptyingMana
import com.wingedsheep.sdk.scripting.PreventManaPoolEmptying
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.matchers.shouldBe

class ManaShortScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Nonproducing test land") { typeLine = "Land" })
        cardRegistry.register(card("Test mana retention") {
            typeLine = "Enchantment"
            staticAbility { ability = PreventManaPoolEmptying }
        })
        cardRegistry.register(card("Test mana conversion") {
            typeLine = "Enchantment"
            staticAbility { ability = ConvertEmptyingMana(Color.BLACK) }
        })

        test("taps all target player's lands including nonproducing lands and removes all floating mana") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Mana Short")
                .withLandsOnBattlefield(1, "Island", 4)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardOnBattlefield(2, "Nonproducing test land")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Test mana retention")
                .withActivePlayer(1).build()
            val pool = ManaPoolComponent(green = 2, blue = 1, colorless = 1)
                .addRestricted(Color.BLUE, 1, ManaRestriction.CreatureSpellsOnly)
                .addRestricted(Color.RED, 2, ManaRestriction.AnySpend, expiry = ManaExpiry.END_OF_COMBAT)
            game.state = game.state.updateEntity(game.player2Id) { it.with(pool) }
            game.castSpellTargetingPlayer(1, "Mana Short", 2).error shouldBe null
            game.resolveStack()
            game.state.getEntity(game.player2Id)!!.get<ManaPoolComponent>() shouldBe ManaPoolComponent()
            for (name in listOf("Forest", "Nonproducing test land")) {
                game.findAllPermanents(name).forEach {
                    game.state.getEntity(it)!!.has<TappedComponent>() shouldBe true
                }
            }
            game.state.getEntity(game.findPermanent("Grizzly Bears")!!)!!.has<TappedComponent>() shouldBe false
            game.findAllPermanents("Island").count {
                game.state.getEntity(it)!!.has<TappedComponent>()
            } shouldBe 3
            game.getLifeTotal(2) shouldBe 20
        }

        test("can target yourself even with an empty pool") {
            val game = scenario().withPlayers().withCardInHand(1, "Mana Short")
                .withLandsOnBattlefield(1, "Island", 4).withActivePlayer(1).build()
            game.castSpellTargetingPlayer(1, "Mana Short", 1).error shouldBe null
            game.resolveStack()
            game.findAllPermanents("Island").all {
                game.state.getEntity(it)!!.has<TappedComponent>()
            } shouldBe true
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.total shouldBe 0
        }

        test("countered Mana Short does not tap lands or remove remaining mana") {
            val game = scenario().withPlayers().withCardInHand(1, "Mana Short")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(2, "Counterspell")
                .withLandsOnBattlefield(2, "Forest", 1).withActivePlayer(1).build()
            game.state = game.state.updateEntity(game.player2Id) { it.with(ManaPoolComponent(blue = 2, green = 2)) }
            game.castSpellTargetingPlayer(1, "Mana Short", 2).error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(2, "Counterspell", "Mana Short").error shouldBe null
            game.resolveStack()
            game.state.getEntity(game.player2Id)!!.get<ManaPoolComponent>()!!.green shouldBe 2
            game.state.getEntity(game.findPermanent("Forest")!!)!!.has<TappedComponent>() shouldBe false
        }

        test("mana conversion replaces forced loss including combat-duration mana") {
            val game = scenario().withPlayers().withCardInHand(1, "Mana Short")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(2, "Test mana conversion").withActivePlayer(1).build()
            val pool = ManaPoolComponent(green = 2, colorless = 1, manaBySource = mapOf(game.findPermanent("Test mana conversion")!! to 3))
                .addRestricted(Color.RED, 2, ManaRestriction.CreatureSpellsOnly, expiry = ManaExpiry.END_OF_COMBAT)
            game.state = game.state.updateEntity(game.player2Id) { it.with(pool) }
            game.castSpellTargetingPlayer(1, "Mana Short", 2).error shouldBe null
            game.resolveStack()
            val after = game.state.getEntity(game.player2Id)!!.get<ManaPoolComponent>()!!
            after.black shouldBe 3
            after.total shouldBe 5
            after.manaBySource shouldBe pool.manaBySource
            after.restrictedMana.all { it.expiry == ManaExpiry.END_OF_COMBAT } shouldBe true
            after.restrictedMana.all { it.color == Color.BLACK && it.restriction == ManaRestriction.CreatureSpellsOnly } shouldBe true
        }
    }
}
