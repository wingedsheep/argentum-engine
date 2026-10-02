package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

class LichScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Lich test life gain") {
            manaCost = "{0}"; typeLine = "Instant"
            spell { effect = Effects.GainLife(3, EffectTarget.Controller) }
        })
        cardRegistry.register(card("Lich test life loss") {
            manaCost = "{0}"; typeLine = "Instant"
            spell { effect = Effects.LoseLife(2, EffectTarget.Controller) }
        })
        cardRegistry.register(card("Lich test damage") {
            manaCost = "{0}"; typeLine = "Instant"
            spell { effect = Effects.DealDamage(2, EffectTarget.Controller) }
        })
        cardRegistry.register(card("Lich test lethal damage") {
            manaCost = "{0}"; typeLine = "Instant"
            spell { effect = Effects.DealDamage(4, EffectTarget.Controller) }
        })

        fun board(life: Int = 0, lichCount: Int = 1, librarySize: Int = 8): TestGame {
            val builder = scenario().withPlayers().withLifeTotal(1, life)
                .withCardInHand(1, "Lich test life gain")
                .withCardInHand(1, "Lich test life loss")
                .withCardInHand(1, "Lich test damage")
                .withCardInHand(1, "Lich test lethal damage")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
            repeat(lichCount) { builder.withCardOnBattlefield(1, "Lich") }
            repeat(librarySize) { builder.withCardInLibrary(1, "Forest") }
            return builder.build()
        }

        test("as enters loses the entire life total without a stack trigger or life-based loss") {
            val game = scenario().withPlayers().withLifeTotal(1, 17)
                .withCardInHand(1, "Lich").withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).build()
            game.castSpell(1, "Lich").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 0
            game.isOnBattlefield("Lich") shouldBe true
            game.state.stack shouldBe emptyList()
            game.state.gameOver shouldBe false
        }

        test("life gain becomes exactly that many draws and emits no life gain") {
            val game = board()
            val before = game.handSize(1)
            game.castSpell(1, "Lich test life gain").error shouldBe null
            val results = game.resolveStack()
            results.flatMap { it.events }.filterIsInstance<LifeChangedEvent>()
                .any { it.reason == LifeChangeReason.LIFE_GAIN } shouldBe false
            game.handSize(1) shouldBe before - 1 + 3
            game.getLifeTotal(1) shouldBe 0
            game.state.stack shouldBe emptyList()
            game.state.gameOver shouldBe false
        }

        test("multiple Liches do not multiply the replacement draws") {
            val game = board(lichCount = 2)
            val before = game.handSize(1)
            game.castSpell(1, "Lich test life gain").error shouldBe null
            game.resolveStack()
            // Any overlapping replacements offer a choice; choose either identical Lich.
            if (game.hasPendingDecision()) {
                game.submitDecision(OptionChosenResponse(game.state.pendingDecision!!.id, 0)).error shouldBe null
                game.resolveStack()
            }
            game.handSize(1) shouldBe before - 1 + 3
            game.getLifeTotal(1) shouldBe 0
        }

        test("life loss can produce negative life and never causes the damage sacrifice trigger") {
            val game = board()
            game.castSpell(1, "Lich test life loss").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe -2
            game.isOnBattlefield("Lich") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.state.gameOver shouldBe false
        }

        test("opponent life gain is not replaced by Lich") {
            val game = scenario().withPlayers().withLifeTotal(1, 0)
                .withCardOnBattlefield(1, "Lich")
                .withCardInHand(2, "Lich test life gain")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(2).build()
            game.castSpell(2, "Lich test life gain").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 23
            game.handSize(2) shouldBe 0
            game.getLifeTotal(1) shouldBe 0
            game.state.gameOver shouldBe false
        }

        test("tokens are excluded from the sacrifice choices") {
            val game = scenario().withPlayers().withLifeTotal(1, 0)
                .withCardOnBattlefield(1, "Lich")
                .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Lich test damage")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).build()
            game.castSpell(1, "Lich test damage").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as com.wingedsheep.engine.core.SelectCardsDecision
            decision.options.contains(game.findPermanent("Grizzly Bears")!!) shouldBe false
            game.selectCards(game.findPermanents("Swamp").take(2)).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.state.gameOver shouldBe false
        }

        test("damage prompts for two nontoken permanents and carries sacrifices into the rider") {
            val game = scenario().withPlayers().withLifeTotal(1, 0)
                .withCardOnBattlefield(1, "Lich")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Lich test damage")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).build()
            game.castSpell(1, "Lich test damage").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe -2
            game.hasPendingDecision() shouldBe true
            game.selectCards(game.findPermanents("Swamp").take(2)).error shouldBe null
            game.resolveStack()
            game.findPermanents("Swamp").size shouldBe 1
            game.findCardsInGraveyard(1, "Swamp").size shouldBe 2
            game.isOnBattlefield("Lich") shouldBe true
            game.state.gameOver shouldBe false
        }

        test("insufficient sacrifices cause explicit loss even at a positive life total") {
            // A token copy still has Lich's abilities but cannot satisfy its nontoken sacrifice.
            // Keeping it on the battlefield isolates the insufficient-sacrifice loss rider.
            val game = scenario().withPlayers().withLifeTotal(1, 20)
                .withCardOnBattlefield(1, "Lich", isToken = true)
                .withCardInHand(1, "Lich test lethal damage")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).build()
            game.castSpell(1, "Lich test lethal damage").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 16
            game.isOnBattlefield("Lich") shouldBe true
            game.state.gameOver shouldBe true
        }

        test("replacement draws from an empty library still lose the game") {
            val game = board(librarySize = 1)
            game.castSpell(1, "Lich test life gain").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 0
            game.state.gameOver shouldBe true
        }

        test("destroying Lich triggers a loss even if its controller has positive life") {
            val game = scenario().withPlayers().withLifeTotal(1, 12)
                .withCardOnBattlefield(1, "Lich")
                .withCardInHand(1, "Disenchant").withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).build()
            game.castSpell(1, "Disenchant", game.findPermanent("Lich")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Lich") shouldBe true
            game.getLifeTotal(1) shouldBe 12
            game.state.gameOver shouldBe true
        }

        test("returning Lich to hand does not trigger its graveyard-only loss") {
            val game = scenario().withPlayers().withLifeTotal(1, 12)
                .withCardOnBattlefield(1, "Lich")
                .withCardInHand(1, "Boomerang").withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).build()
            game.castSpell(1, "Boomerang", game.findPermanent("Lich")!!).error shouldBe null
            game.resolveStack()
            game.findCardsInHand(1, "Lich").size shouldBe 1
            game.getLifeTotal(1) shouldBe 12
            game.state.gameOver shouldBe false
        }
    }
}
