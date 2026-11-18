package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.leg.cards.EvilEyeOfOrmsByGore
import com.wingedsheep.mtg.sets.definitions.leg.cards.Moat
import com.wingedsheep.mtg.sets.definitions.m10.cards.WallOfFrost
import com.wingedsheep.mtg.sets.definitions.m11.cards.StormtideLeviathan
import com.wingedsheep.mtg.sets.definitions.m15.cards.EnsoulArtifact
import com.wingedsheep.mtg.sets.definitions.znr.cards.ClericOfChillDepths
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain

/**
 * Abilities the intent reader left unread:
 * - a block trigger's `DOESNT_UNTAP` grant on the creature it blocked (`TriggeringEntity` — Wall of
 *   Frost, Cleric of Chill Depths): the tapper's lock, as on a chosen target;
 * - an Aura making its host a creature with a real base body (Ensoul Artifact): a pump of the host;
 * - "all lands are Islands" beside the card's own islandwalk (Stormtide Leviathan): evasion;
 * - a battlefield-wide "creatures without flying can't attack" (Moat, Stormtide Leviathan): a shield
 *   over its controller. The own-side form (Evil Eye of Orms-by-Gore) is a price, not a shield.
 */
class AnimateAuraLandwalkAndAttackLockReadingAiTest : FunSpec({

    test("a block trigger's doesn't-untap on the blocked creature reads as a tapper") {
        CardIntentAnalyzer.analyze(WallOfFrost).tags shouldContain IntentTag.TAPPER
        CardIntentAnalyzer.analyze(ClericOfChillDepths).tags shouldContain IntentTag.TAPPER
    }

    test("an Aura that animates its host into a 5/5 reads as a pump") {
        CardIntentAnalyzer.analyze(EnsoulArtifact).tags shouldContain IntentTag.PUMP
    }

    test("all lands are Islands beside islandwalk reads as evasion, the attack lock as protection") {
        CardIntentAnalyzer.analyze(StormtideLeviathan).tags shouldContainAll
            listOf(IntentTag.EVASION_GRANT, IntentTag.PROTECTION)
    }

    test("a battlefield-wide attack lock shields its controller; an own-side one does not") {
        CardIntentAnalyzer.analyze(Moat).tags shouldContain IntentTag.PROTECTION
        CardIntentAnalyzer.analyze(EvilEyeOfOrmsByGore).tags shouldNotContain IntentTag.PROTECTION
    }
})
