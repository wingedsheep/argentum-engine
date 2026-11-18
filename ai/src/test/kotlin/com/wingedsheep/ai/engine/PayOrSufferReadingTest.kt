package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * An "unless target opponent pays" punisher whose suffer never touches the payer — a
 * [com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect] whose penalty is a gift to its
 * controller. Every way out still costs the chosen opponent something (the payment, or what we
 * get), so the slot hurts them, and the ability is for what each way out does for us.
 *
 * The probe — "draw a card unless target opponent sacrifices a creature or pays 3 life" — is the
 * shape at its plainest: our card, an edict, or 3 life, the opponent's pick. No printed card in
 * the corpus has a draw, life or token gift as its whole suffer, so the slot reading is pinned on
 * it. Athreos, God of Passage ("return it to your hand unless target opponent pays 3 life") is the
 * printed one: its ability reads as the creature back or 3 life off the opponent. Its slot stays
 * UNKNOWN, as a return from the graveyard isn't one of the gifts the polarity reader counts.
 *
 * Unread, the probe's slot read UNKNOWN (only a suffer that hurt the payer counted) and neither
 * ability had an intent at all.
 */
class PayOrSufferReadingTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val athreos by lazy {
        cardRegistry.requireCard("Athreos, God of Passage").script.triggeredAbilities.single()
    }

    private val drawProbe by lazy {
        card("Punisher Draw Probe") {
            manaCost = "{2}{B}"
            typeLine = "Sorcery"
            spell {
                val opponent = target(Targets.Opponent)
                effect = Effects.PayOrSuffer(
                    cost = Costs.pay.Choice(
                        listOf(Costs.pay.Sacrifice(filter = GameObjectFilter.Creature), Costs.pay.PayLife(3))
                    ),
                    suffer = Effects.DrawCards(1),
                    player = opponent,
                )
            }
        }.script
    }

    init {
        test("the opponent made to pay or let us draw reads as harmed") {
            withClue("draw unless target opponent sacrifices a creature or pays 3 life") {
                intents.effectTargetPolarities(drawProbe.spellEffect!!, drawProbe.targetRequirements) shouldBe
                    listOf(TargetPolarity.HARMFUL)
            }
        }

        test("the punisher reads as the draw, edict and life loss it forces") {
            withClue("draw unless target opponent sacrifices a creature or pays 3 life: whichever they pick is ours") {
                intents.forEffect(drawProbe.spellEffect!!).tags shouldContainAll
                    setOf(IntentTag.DRAW, IntentTag.REMOVAL, IntentTag.DAMAGE_TO_PLAYERS)
            }
        }

        test("Athreos reads as the recursion and life loss it forces") {
            withClue("Athreos, God of Passage: the dead creature back, or 3 life off the opponent") {
                intents.forEffect(athreos.effect).tags shouldContainAll
                    setOf(IntentTag.RECURSION, IntentTag.DAMAGE_TO_PLAYERS)
            }
        }
    }
}
