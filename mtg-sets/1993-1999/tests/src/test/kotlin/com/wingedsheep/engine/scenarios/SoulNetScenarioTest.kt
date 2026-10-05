package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

class SoulNetScenarioTest : ScenarioTestBase() {
    private val removal = card("Soul Net Test Removal") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.Destroy(permanent)
        }
    }

    init {
        cardRegistry.register(removal)

        for (owner in listOf(1, 2)) {
            test("paying when player $owner's creature dies gains one life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Soul Net")
                    .withCardOnBattlefield(owner, "Grizzly Bears")
                    .withCardInHand(1, removal.name)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, removal.name, game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true).error shouldBe null
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 21
                game.getLifeTotal(2) shouldBe 20
            }
        }

        test("declining a death trigger gains no life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Soul Net")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, removal.name)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, removal.name, game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
        }

        test("a noncreature permanent dying does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Soul Net")
                .withCardOnBattlefield(2, "Sol Ring")
                .withCardInHand(1, removal.name)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, removal.name, game.findPermanent("Sol Ring")!!).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.getLifeTotal(1) shouldBe 20
            game.isOnBattlefield("Sol Ring") shouldBe false
        }
    }
}
