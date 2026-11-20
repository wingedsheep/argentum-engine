package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "This creature can block an additional creature this turn" (Coastline Chimera) — a
 * [com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect] handing out a
 * `CanBlockAdditionalForCreatureGroup` until end of turn — reads as the static itself does,
 * [IntentTag.EXTRA_BLOCKING], and is a payoff spent only in a block: never on our own turn, and on
 * theirs only while blockers are still to be declared.
 *
 * The decision it changes: the Chimera's activation is refused on our own turn, where it can never
 * block, and left open before the opponent's attack.
 */
class GrantedExtraBlockReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val chimera by lazy { cardRegistry.requireCard("Coastline Chimera") }

    private val policy by lazy {
        HoldPolicy(intents, refuseUnspendableGrants = true)
    }

    private fun verdictOn(activePlayer: Int, step: Step): TimingVerdict {
        val game = scenario()
            .withPlayers()
            .withTurnNumber(6)
            .withActivePlayer(activePlayer)
            .withCardOnBattlefield(1, "Coastline Chimera")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .build()
            .advanceToPriority(1, step)
        return policy.verdictFor(
            game.state, game.player1Id, "Coastline Chimera",
            activation = ActivateAbility(
                game.player1Id, game.findPermanent("Coastline Chimera")!!, chimera.script.activatedAbilities[0].id,
            ),
        )
    }

    /** Player 2 attacks with [attackers]; the verdict is read at player 1's declare-attackers priority. */
    private fun verdictAfterAttack(vararg attackers: String): TimingVerdict {
        val game = scenario()
            .withPlayers()
            .withTurnNumber(6)
            .withActivePlayer(2)
            .withCardOnBattlefield(1, "Coastline Chimera")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .build()
            .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
        game.declareAttackers(attackers.associateWith { 1 }).error shouldBe null
        game.advanceToPriority(1, Step.DECLARE_ATTACKERS)
        return policy.verdictFor(
            game.state, game.player1Id, "Coastline Chimera",
            activation = ActivateAbility(
                game.player1Id, game.findPermanent("Coastline Chimera")!!, chimera.script.activatedAbilities[0].id,
            ),
        )
    }

    init {
        test("a granted extra block reads as extra blocking, with a timing") {
            val ability = chimera.script.activatedAbilities[0]
            CardIntentAnalyzer.analyzeEffect(ability.effect, ability.targetRequirements, source = chimera).tags shouldContain
                IntentTag.EXTRA_BLOCKING
            val reading = AiReadingAudit.read(chimera)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("the extra block is refused on our own turn and open before their attack") {
            withClue("on our own turn the Chimera never blocks: the grant wears off unspent") {
                verdictOn(activePlayer = 1, step = Step.PRECOMBAT_MAIN) shouldBe TimingVerdict.NoWindow
            }
            withClue("before the opponent's attack the extra block can still be used") {
                verdictOn(activePlayer = 2, step = Step.BEGIN_COMBAT) shouldNotBe TimingVerdict.NoWindow
            }
        }

        test("once their attack is known, the extra block is only worth it against two or more attackers") {
            withClue("a single attacker needs no second block") {
                verdictAfterAttack("Hill Giant") shouldBe TimingVerdict.NoWindow
            }
            withClue("two attackers give the Chimera a second creature to block") {
                verdictAfterAttack("Hill Giant", "Grizzly Bears") shouldNotBe TimingVerdict.NoWindow
            }
        }
    }
}
