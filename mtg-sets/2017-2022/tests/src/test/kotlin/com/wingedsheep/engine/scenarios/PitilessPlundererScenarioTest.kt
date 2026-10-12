package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class PitilessPlundererScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Plunderer Test Removal") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                val victim = target(TargetFilter.Creature)
                effect = Effects.Destroy(victim)
            }
        })
        cardRegistry.register(card("Plunderer Test Sweeper") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell { effect = Effects.DestroyAll(GameObjectFilter.Creature) }
        })
        cardRegistry.register(card("Plunderer Test Token") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier"))
            }
        })

        fun board() = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Pitiless Plunderer")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(1, "Plunderer Test Removal")
            .withCardInHand(1, "Plunderer Test Sweeper")
            .withCardInHand(1, "Plunderer Test Token")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("another creature you control dying creates a Treasure for you without a decision") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.castSpell(1, "Plunderer Test Removal", bear).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            val treasure = game.findAllPermanents("Treasure").single()
            game.state.projectedState.getController(treasure) shouldBe game.player1Id
            game.state.projectedState.hasSubtype(treasure, "Treasure") shouldBe true
            game.hasPendingDecision() shouldBe false
        }

        test("a creature token dying also creates a Treasure") {
            val game = board()
            game.castSpell(1, "Plunderer Test Token").error shouldBe null
            game.resolveStack()
            val soldier = game.findPermanent("Soldier Token").shouldNotBeNull()
            game.castSpell(1, "Plunderer Test Removal", soldier).error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Soldier Token").size shouldBe 0
            game.findAllPermanents("Treasure").size shouldBe 1
        }

        test("Pitiless Plunderer's own death does not create a Treasure") {
            val game = board()
            val plunderer = game.findPermanent("Pitiless Plunderer").shouldNotBeNull()
            game.castSpell(1, "Plunderer Test Removal", plunderer).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Pitiless Plunderer") shouldBe true
            game.findAllPermanents("Treasure").size shouldBe 0
        }

        test("an opponent's creature dying does not create a Treasure") {
            val game = board()
            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            game.castSpell(1, "Plunderer Test Removal", giant).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.findAllPermanents("Treasure").size shouldBe 0
        }

        test("simultaneous deaths still trigger for your other creatures using last-known information") {
            val game = board()
            game.castSpell(1, "Plunderer Test Token").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Plunderer Test Sweeper").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Pitiless Plunderer") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.findAllPermanents("Treasure").size shouldBe 2
            game.hasPendingDecision() shouldBe false
        }
    }
}
