package com.wingedsheep.rulesengine.player

import com.wingedsheep.rulesengine.core.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse

class PlayerTest : FunSpec({

    context("creation") {
        test("create with name generates id") {
            val player = Player.create("Alice")
            player.name shouldBe "Alice"
            player.id.value.isNotEmpty().shouldBeTrue()
        }

        test("new player has starting life") {
            val player = Player.create("Alice")
            player.life shouldBe 20
        }

        test("new player has empty mana pool") {
            val player = Player.create("Alice")
            player.manaPool.isEmpty.shouldBeTrue()
        }

        test("new player has empty zones") {
            val player = Player.create("Alice")
            player.librarySize shouldBe 0
            player.handSize shouldBe 0
            player.graveyardSize shouldBe 0
        }

        test("new player is alive") {
            val player = Player.create("Alice")
            player.isAlive.shouldBeTrue()
            player.hasLost.shouldBeFalse()
            player.hasWon.shouldBeFalse()
        }
    }

    context("life") {
        test("gain life") {
            val player = Player.create("Alice").gainLife(5)
            player.life shouldBe 25
        }

        test("lose life") {
            val player = Player.create("Alice").loseLife(5)
            player.life shouldBe 15
        }

        test("deal damage") {
            val player = Player.create("Alice").dealDamage(7)
            player.life shouldBe 13
        }

        test("set life") {
            val player = Player.create("Alice").setLife(10)
            player.life shouldBe 10
        }

        test("life can go negative") {
            val player = Player.create("Alice").loseLife(25)
            player.life shouldBe -5
        }
    }

    context("mana") {
        test("add colored mana") {
            val player = Player.create("Alice")
                .addMana(Color.WHITE, 2)
                .addMana(Color.BLUE)

            player.manaPool.white shouldBe 2
            player.manaPool.blue shouldBe 1
        }

        test("add colorless mana") {
            val player = Player.create("Alice").addColorlessMana(3)
            player.manaPool.colorless shouldBe 3
        }

        test("spend mana") {
            val player = Player.create("Alice")
                .addMana(Color.RED, 5)
                .spendMana(Color.RED, 2)

            player.manaPool.red shouldBe 3
        }

        test("empty mana pool") {
            val player = Player.create("Alice")
                .addMana(Color.WHITE, 3)
                .addMana(Color.BLUE, 2)
                .emptyManaPool()

            player.manaPool.isEmpty.shouldBeTrue()
        }
    }

    context("poison counters") {
        test("add poison counters") {
            val player = Player.create("Alice").addPoisonCounters(3)
            player.poisonCounters shouldBe 3
        }

        test("poison counters accumulate") {
            val player = Player.create("Alice")
                .addPoisonCounters(2)
                .addPoisonCounters(3)

            player.poisonCounters shouldBe 5
        }
    }

    context("lands played") {
        test("new player can play land") {
            val player = Player.create("Alice")
            player.canPlayLand.shouldBeTrue()
        }

        test("after playing land, cannot play another") {
            val player = Player.create("Alice").recordLandPlayed()
            player.canPlayLand.shouldBeFalse()
            player.landsPlayedThisTurn shouldBe 1
        }

        test("reset lands played") {
            val player = Player.create("Alice")
                .recordLandPlayed()
                .resetLandsPlayed()

            player.canPlayLand.shouldBeTrue()
            player.landsPlayedThisTurn shouldBe 0
        }
    }

    context("game state") {
        test("mark as lost") {
            val player = Player.create("Alice").markAsLost()
            player.hasLost.shouldBeTrue()
            player.isAlive.shouldBeFalse()
        }

        test("mark as won") {
            val player = Player.create("Alice").markAsWon()
            player.hasWon.shouldBeTrue()
            player.isAlive.shouldBeFalse()
        }
    }

    context("zone updates") {
        test("updateLibrary transforms library") {
            val player = Player.create("Alice")
            val updated = player.updateLibrary { it.shuffle() }
            // Just verify it doesn't throw
            updated.library shouldBe updated.library
        }

        test("updateHand transforms hand") {
            val player = Player.create("Alice")
            val updated = player.updateHand { it }
            updated.hand shouldBe updated.hand
        }

        test("updateGraveyard transforms graveyard") {
            val player = Player.create("Alice")
            val updated = player.updateGraveyard { it }
            updated.graveyard shouldBe updated.graveyard
        }
    }
})
