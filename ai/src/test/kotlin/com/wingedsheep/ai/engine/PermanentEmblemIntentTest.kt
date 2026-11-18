package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.effects.CreatePermanentEmblemEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain

/**
 * "You get an emblem with 'If a source would deal damage to you or a planeswalker you control,
 * prevent all but 1 of that damage'" (Ajani Steadfast's −7): a
 * [com.wingedsheep.sdk.scripting.effects.CreatePermanentEmblemEffect] whose content is a standing
 * replacement effect, not an anthem. Its shield over us and our planeswalkers is the protection the
 * same prevention printed on a permanent reads as; an emblem's mana ability (Liliana of the Dark
 * Realms' Swamps tapping for {B}{B}{B}{B}) is the ramp it is.
 *
 * Unread, only the emblem's stat bonus, keywords and statics counted, so the ultimate had no intent.
 */
class PermanentEmblemIntentTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun emblemOf(card: String) =
        cardRegistry.requireCard(card).script.activatedAbilities.single { it.effect is CreatePermanentEmblemEffect }

    init {
        test("an emblem's damage-prevention shield over us reads as protection") {
            withClue("Ajani Steadfast −7: prevent all but 1 of the damage to us and our planeswalkers") {
                intents.forEffect(emblemOf("Ajani Steadfast").effect).tags shouldContain IntentTag.PROTECTION
            }
        }

        test("an emblem handing our lands a mana ability reads as ramp") {
            withClue("Liliana of the Dark Realms −6: Swamps you control tap for {B}{B}{B}{B}") {
                intents.forEffect(emblemOf("Liliana of the Dark Realms").effect).tags shouldContain IntentTag.RAMP
            }
        }
    }
}
