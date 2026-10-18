package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Eater of Virtue (NEO #245) — {1} Legendary Artifact — Equipment.
 *
 * "Whenever equipped creature dies, exile it. Equipped creature gets +2/+0. As long as a card
 *  exiled with Eater of Virtue has flying, equipped creature has flying. The same is true for
 *  first strike, double strike, deathtouch, haste, hexproof, indestructible, lifelink, menace,
 *  protection, reach, trample, and vigilance. Equip {1}"
 */
class EaterOfVirtueScenarioTest : ScenarioTestBase() {
    init {
        val equipId = cardRegistry.getCard("Eater of Virtue")!!
            .activatedAbilities.single { it.isEquipAbility }.id

        /** Eater equipped to [victim]; Shock kills it, then Eater is re-equipped to Grizzly Bears. */
        fun killThenReequip(victim: String): Pair<TestGame, EntityId> {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, victim)
                .withCardAttachedTo(1, "Eater of Virtue", victim)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val eater = game.findPermanent("Eater of Virtue")!!
            game.castSpell(1, "Shock", game.findPermanent(victim)!!).error shouldBe null
            game.resolveStack()
            withClue("the dead creature is exiled with Eater of Virtue") {
                game.isInExile(1, victim) shouldBe true
                game.state.getEntity(eater)?.get<LinkedExileComponent>()?.exiledIds?.size shouldBe 1
            }
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(game.player1Id, eater, equipId, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()
            return game to bears
        }

        test("equipped creature gets +2/+0 and nothing else while the pile is empty") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Eater of Virtue", "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 2
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
        }

        test("a dead creature's flying passes to the next equipped creature") {
            val (game, bears) = killThenReequip("Suntail Hawk")
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 4
        }

        test("protection is gained with its quality, alongside first strike") {
            val (game, bears) = killThenReequip("White Knight")
            val projected = game.state.projectedState
            projected.hasKeyword(bears, "PROTECTION_FROM_BLACK") shouldBe true
            projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
        }

        test("hexproof from a quality is gained as a hexproof ability") {
            val (game, bears) = killThenReequip("Knight of Grace")
            val projected = game.state.projectedState
            projected.hasKeyword(bears, "HEXPROOF_FROM_BLACK") shouldBe true
            projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
            withClue("hexproof from black is not plain hexproof") {
                projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe false
            }
        }

        test("a card in exile that wasn't exiled with Eater of Virtue grants nothing") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Eater of Virtue", "Grizzly Bears")
                .withCardInExile(2, "Serra Angel")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
            game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
        }
    }
}
