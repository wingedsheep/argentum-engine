package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class ConsecrateLandScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Test Land Aura") {
            manaCost = "{W}"
            typeLine = "Enchantment — Aura"
            auraTarget = TargetObject(filter = TargetFilter.Land)
        })
        cardRegistry.register(card("Test Quick Consecration") {
            manaCost = "{W}"
            typeLine = "Instant"
            spell { val t = target(TargetFilter.PermanentInYourGraveyard); effect = Effects.PutOntoBattlefield(t) }
        })
        test("enchants a land, survives its own prohibition, and prevents destruction") {
            val g = scenario().withPlayers("Player1", "Player2").withCardOnBattlefield(1, "Forest")
                .withCardInHand(1, "Consecrate Land").withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(2, "Sinkhole").withLandsOnBattlefield(2, "Swamp", 2)
                .withActivePlayer(1).build()
            val land = g.findPermanent("Forest")!!
            g.castSpell(1, "Consecrate Land", land).error shouldBe null
            g.resolveStack()
            g.isOnBattlefield("Consecrate Land") shouldBe true
            g.state.projectedState.hasKeyword(land, Keyword.INDESTRUCTIBLE) shouldBe true
            g.state = g.state.copy(activePlayerId = g.player2Id).withPriority(g.player2Id)
            g.castSpell(2, "Sinkhole", land).error shouldBe null
            g.resolveStack()
            g.isOnBattlefield("Forest") shouldBe true
            g.isInGraveyard(2, "Sinkhole") shouldBe true
        }
        test("existing Auras go to their owners' graveyards but Consecrate Land remains") {
            val g = scenario().withPlayers("Player1", "Player2").withCardOnBattlefield(1, "Forest")
                .withCardAttachedTo(2, "Test Land Aura", "Forest")
                .withCardInHand(1, "Consecrate Land").withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1).build()
            g.castSpell(1, "Consecrate Land", g.findPermanent("Forest")!!).error shouldBe null
            g.resolveStack()
            g.isInGraveyard(2, "Test Land Aura") shouldBe true
            g.isOnBattlefield("Consecrate Land") shouldBe true
        }
        test("another Consecrate Land cannot target the protected land") {
            val g = scenario().withPlayers("Player1", "Player2").withCardOnBattlefield(1, "Forest")
                .withCardAttachedTo(1, "Consecrate Land", "Forest")
                .withCardInHand(1, "Consecrate Land").withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1).build()
            g.castSpell(1, "Consecrate Land", g.findPermanent("Forest")!!).error.shouldNotBeNull()
            g.state.stack.size shouldBe 0
        }
        test("an Aura spell loses its target when Consecrate Land enters in response") {
            val g = scenario().withPlayers("Player1", "Player2").withCardOnBattlefield(1, "Forest")
                .withCardInHand(1, "Test Land Aura").withLandsOnBattlefield(1, "Plains", 1)
                .withCardInGraveyard(2, "Consecrate Land")
                .withCardInHand(2, "Test Quick Consecration").withLandsOnBattlefield(2, "Plains", 1)
                .withActivePlayer(1).build()
            val land = g.findPermanent("Forest")!!
            g.castSpell(1, "Test Land Aura", land).error shouldBe null
            g.state = g.state.withPriority(g.player2Id)
            g.castSpellTargetingGraveyardCard(2, "Test Quick Consecration", 2, "Consecrate Land").error shouldBe null
            // Resolve the instant, then select the non-targeted host before the original Aura resolves.
            g.resolveStack()
            val choice = g.state.pendingDecision as com.wingedsheep.engine.core.ChooseTargetsDecision
            g.submitDecision(com.wingedsheep.engine.core.TargetsResponse(choice.id,
                mapOf(0 to listOf(land)))).error shouldBe null
            g.resolveStack()
            g.isOnBattlefield("Consecrate Land") shouldBe true
            g.isInGraveyard(1, "Test Land Aura") shouldBe true
        }
        test("destroying the Aura restores enchantment and destruction legality") {
            val g = scenario().withPlayers("Player1", "Player2").withCardOnBattlefield(1, "Forest")
                .withCardAttachedTo(1, "Consecrate Land", "Forest")
                .withCardInHand(1, "Disenchant").withCardInHand(1, "Test Land Aura")
                .withLandsOnBattlefield(1, "Plains", 3).withActivePlayer(1).build()
            val land = g.findPermanent("Forest")!!
            g.castSpell(1, "Disenchant", g.findPermanent("Consecrate Land")!!).error shouldBe null
            g.resolveStack()
            g.state.projectedState.hasKeyword(land, Keyword.INDESTRUCTIBLE) shouldBe false
            g.castSpell(1, "Test Land Aura", land).error shouldBe null
            g.resolveStack()
            g.state.getEntity(g.findPermanent("Test Land Aura")!!)!!.get<AttachedToComponent>()!!.targetId shouldBe land
        }
    }
}
