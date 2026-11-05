package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Static abilities that change how spells are cast or how creatures fight, which the intent reader
 * had no entry for, so Reality Fracture's Omnipresence, Ghalta the Immovable, Samut, Tyrant of
 * Naktamun and Thalia, the Survivor (and their kin across the catalog) were each "abilities not
 * recognised":
 * - `MayCastWithoutPayingManaCost` for its controller alone → FREE_SPELL;
 * - `AssignDamageEqualToToughness` / `CanAttackDespiteDefender` on our own creatures → COMBAT_ENHANCEMENT;
 * - `GrantKeywordToOwnSpells`, read by what the keyword does to the spell;
 * - `ModifySpellCost` raising what opponents' spells cost → SPELL_TAX.
 *
 * Unread, Omnipresence sat on the board at an unreadable permanent's 0.5, below a Levitation, so a
 * Disenchant went at the Levitation and left the opponent casting their hand for free.
 */
class CastingAndCombatStaticsReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!
    private fun tags(name: String) = CardIntentAnalyzer.analyze(card(name)).tags

    init {
        test("at the opponent's end step, Disenchant answers their free-spell engine before a Levitation") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Disenchant")
                .withCardOnBattlefield(2, "Omnipresence")
                .withCardOnBattlefield(2, "Levitation")
                .build()
                .advanceToPriority(1, Step.END)

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            val cast = withClue("chose $action") { action.shouldBeInstanceOf<CastSpell>() }
            val target = (cast.targets.single() as ChosenTarget.Permanent).entityId
            withClue("Omnipresence reads as a standing free cast (FREE_SPELL), worth more than a flying grant") {
                game.state.getEntity(target)!!.get<CardComponent>()!!.name shouldBe "Omnipresence"
            }
        }

        test("a free-cast permission reads as a free spell only when it is ours alone") {
            listOf("Omnipresence", "Fires of Invention", "Zaffai and the Tempests").forEach { name ->
                withClue("$name: a standing free cast for its controller") { tags(name) shouldContain IntentTag.FREE_SPELL }
            }
            withClue("Weftwalking's free cast is every player's") { tags("Weftwalking") shouldNotContain IntentTag.FREE_SPELL }
        }

        test("damage by toughness and attacking despite defender are combat enhancements on our side only") {
            listOf("Ghalta the Immovable", "High Alert").forEach { name ->
                withClue(name) { tags(name) shouldContain IntentTag.COMBAT_ENHANCEMENT }
            }
            withClue("Doran's every-creature reach helps the opponent's walls as much") {
                tags("Doran, the Siege Tower") shouldNotContain IntentTag.COMBAT_ENHANCEMENT
            }
        }

        test("a keyword granted to our spells reads by what it does to the spell") {
            withClue("split second: our spells can't be answered") {
                tags("Samut, Tyrant of Naktamun") shouldContain IntentTag.PROTECTION
            }
            withClue("storm copies the spell") { tags("Prismari, the Inspiration") shouldContain IntentTag.SPELL_COPY }
            withClue("convoke pays for the spell") { tags("Eirdu, Carrier of Dawn") shouldContain IntentTag.COST_REDUCER }
            withClue("lifelink on spells gains life") { tags("Radiant Scrollwielder") shouldContain IntentTag.LIFEGAIN }
        }

        test("a tax on the opponent's spells reads as a spell tax, never as our discount") {
            listOf("Thalia, the Survivor", "God-Pharaoh's Statue").forEach { name ->
                withClue(name) {
                    tags(name) shouldContain IntentTag.SPELL_TAX
                    tags(name) shouldNotContain IntentTag.COST_REDUCER
                }
            }
        }
    }
}
