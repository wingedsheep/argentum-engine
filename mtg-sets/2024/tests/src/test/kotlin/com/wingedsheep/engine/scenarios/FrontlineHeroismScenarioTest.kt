package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Frontline Heroism — "When this enchantment enters, create a 1/1 red Soldier creature token with
 * haste. Whenever you cast a spell that targets only a single creature you control, create a 1/1 red
 * Soldier creature token with haste, then copy that spell. The copy targets that token."
 *
 * The copy is CR 707.10e's specified new target: no copy when the token isn't a legal target, and
 * with a token doubler the copy targets the one token chosen.
 */
class FrontlineHeroismScenarioTest : ScenarioTestBase() {

    init {
        test("entering creates a 1/1 red Soldier token with haste") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Frontline Heroism")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Frontline Heroism").error shouldBe null
            game.resolveStack()

            val soldier = game.findPermanents("Soldier Token").shouldHaveSize(1).single()
            val projected = game.state.projectedState
            projected.getPower(soldier) shouldBe 1
            projected.getToughness(soldier) shouldBe 1
            projected.hasKeyword(soldier, Keyword.HASTE) shouldBe true
        }

        test("a spell on a single creature you control makes a Soldier and a copy aimed at it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Frontline Heroism")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            val soldier = game.findPermanents("Soldier Token").shouldHaveSize(1).single()
            val projected = game.state.projectedState
            withClue("the copy pumps the token") { projected.getPower(soldier) shouldBe 4 }
            withClue("the original still pumps the Bears") { projected.getPower(bears) shouldBe 5 }
        }

        test("a spell targeting an opponent's creature doesn't trigger it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Frontline Heroism")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            withClue("only Shock is on the stack") { game.state.stack.size shouldBe 1 }
            game.resolveStack()

            game.findPermanents("Soldier Token") shouldHaveSize 0
        }

        test("when the token isn't a legal target the Soldier is made but no copy is (707.10e)") {
            // Smite the Monstrous: "Destroy target creature with power 4 or greater." The 1/1 token
            // can't be its target, so the spell isn't copied and the token survives.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Frontline Heroism")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardInHand(1, "Smite the Monstrous")
                .withLandsOnBattlefield(1, "Plains", 4)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Smite the Monstrous", game.findPermanent("Craw Wurm")!!).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findPermanents("Soldier Token") shouldHaveSize 1
            game.findPermanent("Craw Wurm") shouldBe null
        }

        test("with a token doubler you choose one of the two Soldiers and only it gets the copy") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Frontline Heroism")
                .withCardOnBattlefield(1, "Anointed Procession")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()

            val soldiers = game.findPermanents("Soldier Token").shouldHaveSize(2)
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.toSet() shouldBe soldiers.toSet()
            game.selectCards(listOf(soldiers.first())).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(soldiers.first()) shouldBe 4
            projected.getPower(soldiers.last()) shouldBe 1
            projected.getPower(bears) shouldBe 5
        }

        test("an Aura spell is copied too, and its copy becomes a token Aura on the Soldier") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Frontline Heroism")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Holy Strength")
                .withLandsOnBattlefield(1, "Plains", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Holy Strength", bears).error shouldBe null
            game.resolveStack()

            val soldier = game.findPermanents("Soldier Token").single()
            val auras = game.findPermanents("Holy Strength").shouldHaveSize(2)
            auras.map { game.state.getEntity(it)?.get<AttachedToComponent>()?.targetId }.toSet() shouldBe
                setOf(bears, soldier)
            val projected = game.state.projectedState
            projected.getPower(soldier) shouldBe 2
            projected.getToughness(soldier) shouldBe 3
            projected.getPower(bears) shouldBe 3
        }
    }
}
