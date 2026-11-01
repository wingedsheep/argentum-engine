package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Destroy all creatures" — a battlefield-wide gather moved to the graveyard by destruction or
 * sacrifice ([com.wingedsheep.sdk.scripting.effects.GatherCardsEffect] from
 * [com.wingedsheep.sdk.scripting.effects.CardSource.BattlefieldMatching] into a
 * [com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect]): Day of Judgment, Damnation,
 * Pernicious Deed, Child of Alara's death trigger.
 *
 * Only the `ForEach` spelling of a wrath was read as a sweeper; the pipeline spelling, which
 * `Effects.DestroyAll` lowers to, read as nothing at all. So every consumer that prices removal
 * in hand saw a do-nothing card.
 */
class DestroyAllReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a destroy-all pipeline is a sweeper") {
            for (name in listOf("Day of Judgment", "Damnation", "Pernicious Deed", "Child of Alara")) {
                withClue("$name: destroying a battlefield-wide group reads as a sweeper") {
                    intents.forName(name)!!.tags shouldContainAll listOf(IntentTag.REMOVAL, IntentTag.SWEEPER)
                }
            }
        }

        test("a picked victim is removal, not a sweeper") {
            withClue("Haphazard Bombardment destroys one of the permanents it picked") {
                val tags = intents.forName("Haphazard Bombardment")!!.tags
                tags shouldContainAll listOf(IntentTag.REMOVAL)
                tags shouldNotContain IntentTag.SWEEPER
            }
        }

        test("answers an opposing Day of Judgment by giving its creatures indestructible") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(2)
                .withLandsOnBattlefield(2, "Plains", 4)
                .withCardInHand(2, "Day of Judgment")
                .withCardOnBattlefield(1, "Selfless Glyphweaver")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Craw Wurm")
                .build()
                .also { it.castSpell(2, "Day of Judgment") }
                .advanceToStackResponse(1)

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            val source = (action as? ActivateAbility)?.let { game.state.getEntity(it.sourceId)?.get<CardComponent>()?.name }
            withClue("chose $action — Day of Judgment on the stack reads as a sweeper, the deadline for an until-end-of-turn indestructible grant") {
                source shouldBe "Selfless Glyphweaver"
            }
        }
    }
}
