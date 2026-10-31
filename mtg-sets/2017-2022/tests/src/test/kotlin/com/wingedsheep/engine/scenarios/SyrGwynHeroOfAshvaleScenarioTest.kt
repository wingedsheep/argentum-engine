package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Syr Gwyn, Hero of Ashvale — {3}{R}{W}{B} 5/5 legendary Human Knight.
 *
 * - Whenever an equipped creature you control attacks, you draw a card and you lose 1 life —
 *   once per equipped attacker, however many Equipment it carries.
 * - Equipment you control have equip Knight {0} — the first granted "Equip [quality]" ability:
 *   free onto a Knight, not usable on a non-Knight.
 */
class SyrGwynHeroOfAshvaleScenarioTest : ScenarioTestBase() {

    private fun grantedEquipId() = cardRegistry.getCard("Syr Gwyn, Hero of Ashvale")!!
        .script.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.id

    init {
        test("granted equip Knight {0} attaches an Equipment to a Knight for no mana") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Syr Gwyn, Hero of Ashvale")
                .withCardOnBattlefield(1, "Bonesplitter")
                .withCardOnBattlefield(1, "Youthful Knight")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val splitter = game.findPermanent("Bonesplitter")!!
            val knight = game.findPermanent("Youthful Knight")!!

            game.execute(
                ActivateAbility(game.player1Id, splitter, grantedEquipId(), targets = listOf(ChosenTarget.Permanent(knight)))
            ).error shouldBe null
            game.resolveStack()

            game.state.getEntity(splitter)?.get<AttachedToComponent>()?.targetId shouldBe knight
        }

        test("granted equip Knight can't target a non-Knight creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Syr Gwyn, Hero of Ashvale")
                .withCardOnBattlefield(1, "Bonesplitter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val splitter = game.findPermanent("Bonesplitter")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, splitter, grantedEquipId(), targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldNotBe null
            game.state.getEntity(splitter)?.get<AttachedToComponent>() shouldBe null
        }

        test("an equipped attacker draws one card and loses 1 life, however many Equipment it carries") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Syr Gwyn, Hero of Ashvale")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                .withCardAttachedTo(1, "Leonin Scimitar", "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val handBefore = game.handSize(1)

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Syr Gwyn, Hero of Ashvale" to 2)).error shouldBe null
            game.resolveStack()

            withClue("one trigger for the one equipped attacker; unequipped Syr Gwyn doesn't trigger it") {
                game.handSize(1) shouldBe handBefore + 1
                game.getLifeTotal(1) shouldBe 19
            }
        }
    }
}
