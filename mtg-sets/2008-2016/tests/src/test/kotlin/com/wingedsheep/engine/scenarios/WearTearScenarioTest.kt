package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.mechanics.SplitSpellCharacteristicsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class WearTearScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Test Artifact Enchantment") { manaCost = "{1}"; typeLine = "Artifact Enchantment" })
        fun board(mountains: Int = 2, plains: Int = 2): TestGame = scenario()
            .withPlayers("Caster", "Opponent").withCardInHand(1, "Wear // Tear")
            .withLandsOnBattlefield(1, "Mountain", mountains).withLandsOnBattlefield(1, "Plains", plains)
            .withLandsOnBattlefield(2, "Island", 2).withCardInHand(2, "Counterspell")
            .withCardOnBattlefield(2, "Sol Ring").withCardOnBattlefield(2, "Intangible Virtue")
            .withCardOnBattlefield(2, "Test Artifact Enchantment")
            .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Island")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun TestGame.wearId() = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Wear // Tear" }
        fun TestGame.targets() = listOf(ChosenTarget.Permanent(findPermanent("Sol Ring")!!), ChosenTarget.Permanent(findPermanent("Intangible Virtue")!!))

        test("the action menu offers both halves and fuse with two target groups") {
            val game = board(); val actions = game.getLegalActions(1)
            actions.any { it.description == "Cast Wear" } shouldBe true
            actions.any { it.description == "Cast Tear" } shouldBe true
            val fused = actions.first { (it.action as? CastSpell)?.castFused == true }
            fused.description shouldBe "Cast Wear // Tear (fused)"
            fused.targetRequirements!!.size shouldBe 2
            fused.manaCostString shouldBe "{1}{R}{W}"
        }
        test("fuse is one multicolored spell with combined mana value and destroys both targets") {
            val game = board(); val id = game.wearId()
            val cast = game.execute(CastSpell(game.player1Id, id, targets = game.targets(), castFused = true))
            cast.error.shouldBeNull(); game.state.stack.size shouldBe 1
            game.state.getEntity(id)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.RED, Color.WHITE)
            game.state.getEntity(id)!!.get<CardComponent>()!!.manaValue shouldBe 3
            cast.events.filterIsInstance<SpellCastEvent>().single().manaValue shouldBe 3
            game.state.spellsCastThisTurn shouldBe 1
            game.resolveStack()
            game.findPermanent("Sol Ring").shouldBeNull(); game.findPermanent("Intangible Virtue").shouldBeNull()
            game.state.getGraveyard(game.player1Id).contains(id) shouldBe true
            game.state.getEntity(id)!!.get<SplitSpellCharacteristicsComponent>().shouldBeNull()
        }
        test("Wear needs only red mana and has its own stack characteristics") {
            val game = board(2, 0); val id = game.wearId()
            game.execute(CastSpell(game.player1Id, id, faceIndex = 0, targets = listOf(game.targets().first()))).error.shouldBeNull()
            game.state.getEntity(id)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.RED)
            game.state.getEntity(id)!!.get<CardComponent>()!!.manaValue shouldBe 2
            game.resolveStack(); game.findPermanent("Sol Ring").shouldBeNull(); game.findPermanent("Intangible Virtue").shouldNotBeNull()
        }
        test("Tear needs only white mana") {
            val game = board(0, 1); val id = game.wearId()
            game.execute(CastSpell(game.player1Id, id, faceIndex = 1, targets = listOf(game.targets()[1]))).error.shouldBeNull()
            game.state.getEntity(id)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.WHITE)
            game.state.getEntity(id)!!.get<CardComponent>()!!.manaValue shouldBe 1
            game.resolveStack(); game.findPermanent("Sol Ring").shouldNotBeNull(); game.findPermanent("Intangible Virtue").shouldBeNull()
        }
        test("both halves may target the same artifact enchantment") {
            val game = board(); val id = game.wearId(); val target = ChosenTarget.Permanent(game.findPermanent("Test Artifact Enchantment")!!)
            game.execute(CastSpell(game.player1Id, id, targets = listOf(target, target), castFused = true)).error.shouldBeNull()
            game.resolveStack(); game.findPermanent("Test Artifact Enchantment").shouldBeNull()
        }
        test("an illegal left target does not shift the right target") {
            val game = board(); val id = game.wearId(); val targets = game.targets()
            game.execute(CastSpell(game.player1Id, id, targets = targets, castFused = true)).error.shouldBeNull()
            game.state = game.state.removeEntity(targets.first().entityId)
            game.resolveStack(); game.findPermanent("Intangible Virtue").shouldBeNull()
        }
        test("all illegal targets stop the whole spell") {
            val game = board(); val id = game.wearId(); val targets = game.targets()
            game.execute(CastSpell(game.player1Id, id, targets = targets, castFused = true)).error.shouldBeNull()
            targets.forEach { game.state = game.state.removeEntity(it.entityId) }
            game.resolveStack(); game.state.getGraveyard(game.player1Id).contains(id) shouldBe true
            game.state.getEntity(id)!!.get<SplitSpellCharacteristicsComponent>().shouldBeNull()
        }
        test("fuse requires both targets and both costs") {
            val game = board(1, 0)
            game.execute(CastSpell(game.player1Id, game.wearId(), targets = game.targets(), castFused = true)).error.shouldNotBeNull()
            game.state.getHand(game.player1Id).size shouldBe 1
            val full = board()
            full.execute(CastSpell(full.player1Id, full.wearId(), targets = listOf(full.targets().first()), castFused = true)).error.shouldNotBeNull()
        }
        test("a free cast from hand may be fused") {
            val game = board(0, 0); val id = game.wearId()
            game.state = game.state.updateEntity(id) { it.with(PlayWithoutPayingCostComponent(game.player1Id)) }
            game.getLegalActions(1).any { (it.action as? CastSpell)?.castFused == true } shouldBe true
            game.execute(CastSpell(game.player1Id, id, targets = game.targets(), castFused = true)).error.shouldBeNull()
            game.resolveStack(); game.findPermanent("Sol Ring").shouldBeNull(); game.findPermanent("Intangible Virtue").shouldBeNull()
        }
        test("a fused spell is countered as one spell and restores its card") {
            val game = board(); val id = game.wearId()
            game.execute(CastSpell(game.player1Id, id, targets = game.targets(), castFused = true)).error.shouldBeNull()
            game.passPriority()
            val counter = game.state.getHand(game.player2Id).single()
            game.execute(CastSpell(game.player2Id, counter, targets = listOf(ChosenTarget.Spell(id)))).error.shouldBeNull()
            game.resolveStack()
            game.findPermanent("Sol Ring").shouldNotBeNull(); game.findPermanent("Intangible Virtue").shouldNotBeNull()
            game.state.getGraveyard(game.player1Id).contains(id) shouldBe true
            game.state.getEntity(id)!!.get<SpellOnStackComponent>().shouldBeNull()
            game.state.getEntity(id)!!.get<SplitSpellCharacteristicsComponent>().shouldBeNull()
        }
    }
}
