package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Aura half of [AiProfile.refuseWastedPayoffs]'s cast check: Earthbind's "if enchanted creature
 * has flying" entry trigger never fires on a non-flyer, so casting it there buys nothing.
 */
class RefuseUnmetAuraEntryTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.castName(action: GameAction): String? =
        (action as? CastSpell)?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }

    private fun TestGame.verdictForEarthbindOn(creature: String, refuseWastedPayoffs: Boolean = true): TimingVerdict {
        val card = state.getZone(player1Id, Zone.HAND).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Earthbind"
        }
        val target = findPermanent(creature)!!
        return HoldPolicy(intents, refuseWastedPayoffs = refuseWastedPayoffs).verdictFor(
            state, player1Id, "Earthbind",
            cast = CastSpell(player1Id, card, targets = listOf(ChosenTarget.Permanent(target))),
        )
    }

    private fun position(vararg theirCreatures: String) = scenario()
        .withPlayers()
        .withTurnNumber(5)
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardInHand(1, "Earthbind")
        .withLandsOnBattlefield(2, "Island", 3)
        .apply { theirCreatures.forEach { withCardOnBattlefield(2, it) } }
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("Earthbind is not cast on a creature without flying") {
            val game = position("Craw Wurm")

            game.verdictForEarthbindOn("Craw Wurm") shouldBe TimingVerdict.NoWindow
            game.verdictForEarthbindOn("Craw Wurm", refuseWastedPayoffs = false) shouldNotBe TimingVerdict.NoWindow
            game.castName(game.choice()) shouldNotBe "Earthbind"
        }

        test("on a flyer it is an ordinary removal Aura again") {
            val game = position("Craw Wurm", "Wind Drake")

            game.verdictForEarthbindOn("Wind Drake") shouldNotBe TimingVerdict.NoWindow
            game.verdictForEarthbindOn("Craw Wurm") shouldBe TimingVerdict.NoWindow
            val action = game.choice()
            game.castName(action) shouldBe "Earthbind"
            ((action as CastSpell).targets.single() as ChosenTarget.Permanent).entityId shouldBe
                game.findPermanent("Wind Drake")
        }
    }
}
