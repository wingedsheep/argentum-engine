package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class PlayerDamageIntentTest : ScenarioTestBase() {
    init {
        test("delayed damage to a triggering creature's controller is player damage without creature reach") {
            val intent = CardIntentAnalyzer.analyze(cardRegistry.getCard("Harsh Justice")!!)
            intent.tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
            intent.tags shouldNotContain IntentTag.REMOVAL
            intent.removalReach shouldBe null
        }

        test("player damage describes its recipient without claiming removal or an offensive payoff") {
            for (target in listOf(
                EffectTarget.PlayerRef(Player.EachOpponent),
                EffectTarget.PlayerRef(Player.You),
                EffectTarget.ControllerOfTriggeringEntity,
                EffectTarget.ControllerOfDamageSource,
            )) {
                val intent = CardIntentAnalyzer.analyzeEffect(Effects.DealDamage(3, target))
                intent.tags shouldBe setOf(IntentTag.DAMAGE_TO_PLAYERS)
                intent.removalReach shouldBe null
            }
            val self = CardIntentAnalyzer.analyzeEffect(Effects.DealDamage(3, EffectTarget.Self))
            self.tags shouldNotContain IntentTag.DAMAGE_TO_PLAYERS
            self.tags shouldNotContain IntentTag.REMOVAL
        }

        test("a symmetric life loss drains the opponents too, while losing only our own life does not") {
            // Rabid Bloodsucker's "when this creature enters, each player loses 2 life".
            CardIntentAnalyzer.analyze(cardRegistry.getCard("Rabid Bloodsucker")!!).tags shouldContain
                IntentTag.DAMAGE_TO_PLAYERS
            CardIntentAnalyzer.analyzeEffect(Effects.LoseLife(2, EffectTarget.PlayerRef(Player.Each))).tags shouldBe
                setOf(IntentTag.DAMAGE_TO_PLAYERS)
            CardIntentAnalyzer.analyzeEffect(Effects.LoseLife(2, EffectTarget.Controller)).tags shouldNotContain
                IntentTag.DAMAGE_TO_PLAYERS
        }
    }
}
