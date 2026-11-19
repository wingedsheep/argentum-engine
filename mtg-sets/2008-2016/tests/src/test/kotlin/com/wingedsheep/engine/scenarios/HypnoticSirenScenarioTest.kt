package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hypnotic Siren: bestowed onto an opponent's creature, its caster controls that creature, which
 * gets +1/+1 and flying. When the stolen creature leaves, the Siren falls off and stays as its
 * owner's 1/1 flyer.
 */
class HypnoticSirenScenarioTest : ScenarioTestBase() {

    init {
        test("bestowed on an opponent's creature steals it and grants +1/+1 and flying; falls off as a 1/1 flyer") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Hypnotic Siren")
                .withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Island", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Hypnotic Siren").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val siren = game.findPermanent("Hypnotic Siren")!!
            game.state.getEntity(siren)?.get<AttachedToComponent>()?.targetId shouldBe bears
            val projected = game.state.projectedState
            projected.getController(bears) shouldBe game.player1Id
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            projected.isCreature(siren) shouldBe false

            game.castSpell(1, "Unsummon", bears).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Grizzly Bears") shouldBe true
            val fallen = game.findPermanent("Hypnotic Siren")
            fallen shouldNotBe null
            val after = game.state.projectedState
            after.isCreature(fallen!!) shouldBe true
            after.getController(fallen) shouldBe game.player1Id
            after.getPower(fallen) shouldBe 1
            after.hasKeyword(fallen, Keyword.FLYING) shouldBe true
        }
    }
}
