package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Amass ([com.wingedsheep.sdk.scripting.effects.AmassEffect]): "put N +1/+1 counters on an Army
 * you control; if you don't control one, create a 0/0 Army creature token first." Every amass is
 * a body on the battlefield — a new token the first time, a bigger one after — so it reads as a
 * token maker, the same as the token it makes.
 *
 * Unread, a permanent that amasses every turn (Dreadhorde Invasion, Barad-dûr) was priced as a
 * permanent nothing reads, below an unattached Equipment, so removal went to the wrong one.
 */
class AmassReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("an amass reads as a token maker") {
            for (name in listOf("Dreadhorde Invasion", "Barad-dûr", "Grim Initiate", "Invade the City")) {
                withClue("$name: amassing puts an Army body on the battlefield") {
                    intents.forName(name)!!.tags shouldContain IntentTag.TOKEN_MAKER
                }
            }
        }

        test("Disenchant goes to the enchantment that amasses every upkeep, not an idle Equipment") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Disenchant")
                .withCardOnBattlefield(2, "Dreadhorde Invasion")
                .withCardOnBattlefield(2, "Leonin Scimitar")
                .build()

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            val target = (action as? CastSpell)?.targets?.firstOrNull()?.let { t ->
                (t as? ChosenTarget.Permanent)?.let { game.state.getEntity(it.entityId) }?.get<CardComponent>()?.name
            }
            withClue("chose $action — Dreadhorde Invasion amasses an Army every upkeep: a repeatable token maker") {
                target shouldBe "Dreadhorde Invasion"
            }
        }
    }
}
