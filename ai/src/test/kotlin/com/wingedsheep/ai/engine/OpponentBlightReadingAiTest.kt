package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * An opponent blighting (`Patterns.Mechanic.blight` aimed at another player: gather the creatures
 * they control, they pick one, an [com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect]
 * puts -1/-1 counters on it) is an edict that shrinks instead of sacrificing: removal, and a
 * target-opponent slot that hurts the player chosen.
 *
 * Unread, High Perfect Morcant's "each opponent blights 1" earned no intent at all and Champion of
 * the Weird's "target opponent blights 2" slot read UNKNOWN. Blighting our own creature stays a
 * price, not removal.
 */
class OpponentBlightReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("each opponent blighting reads as removal") {
            val morcant = cardRegistry.requireCard("High Perfect Morcant")
            withClue("High Perfect Morcant: each opponent puts a -1/-1 counter on a creature they control") {
                intents.forEffect(morcant.script.triggeredAbilities.single().effect).tags shouldContain IntentTag.REMOVAL
            }
        }

        test("target opponent blighting hurts the opponent chosen") {
            val champion = cardRegistry.requireCard("Champion of the Weird")
            val ability = champion.script.activatedAbilities.single()
            withClue("Champion of the Weird: 'target opponent blights 2' is aimed at the player who loses the creature") {
                intents.abilityTargetPolarities(champion.name, ability.id) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("and it is removal") {
                intents.forEffect(ability.effect).tags shouldContain IntentTag.REMOVAL
            }
        }

        test("blighting our own creature is a price, not removal") {
            val urchin = cardRegistry.requireCard("Shadow Urchin")
            withClue("Shadow Urchin: 'whenever this attacks, blight 1' shrinks one of ours") {
                intents.forEffect(urchin.script.triggeredAbilities.first().effect).tags shouldNotContain IntentTag.REMOVAL
            }
        }
    }
}
