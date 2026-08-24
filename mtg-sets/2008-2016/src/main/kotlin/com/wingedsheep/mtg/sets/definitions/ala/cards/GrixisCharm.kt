package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Grixis Charm
 * {U}{B}{R}
 * Instant
 *
 * Choose one —
 * • Return target permanent to its owner's hand.
 * • Target creature gets -4/-4 until end of turn.
 * • Creatures you control get +2/+0 until end of turn.
 *
 * A plain choose-one `modal(chooseCount = 1)`. The bounce mode is [Effects.ReturnToHand] over an
 * unfiltered [TargetPermanent]; the shrink mode is [Effects.ModifyStats]`(-4, -4)` on a
 * [TargetCreature]; the team pump is [Patterns.Group]`.modifyStatsForAll` over
 * [GroupFilter.AllCreaturesYouControl], which lowers to the same per-creature `ModifyStats` the
 * group iteration expects — no group-only effect type needed.
 */
val GrixisCharm = card("Grixis Charm") {
    manaCost = "{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Return target permanent to its owner's hand.\n" +
        "• Target creature gets -4/-4 until end of turn.\n" +
        "• Creatures you control get +2/+0 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Return target permanent to its owner's hand") {
                val t = target("target", TargetPermanent())
                effect = Effects.ReturnToHand(t)
            }
            mode("Target creature gets -4/-4 until end of turn") {
                val t = target("target", TargetCreature())
                effect = Effects.ModifyStats(-4, -4, t)
            }
            mode("Creatures you control get +2/+0 until end of turn") {
                effect = Patterns.Group.modifyStatsForAll(2, 0, GroupFilter.AllCreaturesYouControl)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Lars Grant-West"
        flavorText = "\"So many choices. Shall I choose loathing, hate, or malice today?\"\n—Eliza of the Keep"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a98a6695-44ce-445f-b476-e18a0b5dd8f2.jpg"
        ruling("2008-10-01", "You can choose a mode only if you can choose legal targets for that mode. If you can’t choose legal targets for any of the modes, you can’t cast the spell.")
        ruling("2008-10-01", "While the spell is on the stack, treat it as though its only text is the chosen mode. The other two modes are treated as though they don’t exist. You don’t choose targets for those modes.")
        ruling("2008-10-01", "If this spell is copied, the copy will have the same mode as the original.")
    }
}
