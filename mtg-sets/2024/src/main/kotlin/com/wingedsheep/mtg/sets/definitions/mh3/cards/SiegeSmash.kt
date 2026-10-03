package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Siege Smash
 * {1}{R}
 * Instant
 * Split second
 * Choose one —
 * • Destroy target artifact.
 * • Target creature gets +3/+2 and gains trample until end of turn.
 *
 * Split second is the printed keyword; the engine's single read point (`SplitSecond`) locks out
 * casting and non-mana activations while the spell is on the stack (CR 702.61). The modes are a
 * plain choose-one `modal`, one target each.
 */
val SiegeSmash = card("Siege Smash") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Split second (As long as this spell is on the stack, players can't cast spells or activate " +
        "abilities that aren't mana abilities.)\nChoose one —\n• Destroy target artifact.\n" +
        "• Target creature gets +3/+2 and gains trample until end of turn."

    keywords(Keyword.SPLIT_SECOND)

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target artifact") {
                val t = target(TargetFilter.Artifact)
                effect = Effects.Destroy(t)
            }
            mode("Target creature gets +3/+2 and gains trample until end of turn") {
                val t = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(3, 2, t) then Effects.GrantKeyword(Keyword.TRAMPLE, t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Joshua Cairos"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f33e3b25-76f5-4263-a309-9ea97f2d8248.jpg?1783911266"
        ruling("2024-06-07", "Players still get priority while a spell with split second is on the stack; their options are just limited to mana abilities and certain special actions.")
        ruling("2024-06-07", "Split second doesn't stop triggered abilities from triggering. If one does, its controller puts it on the stack and chooses targets for it, if any. Those abilities will resolve as normal.")
        ruling("2024-06-07", "After a spell with split second resolves (or otherwise leaves the stack), players may again cast spells and activate abilities before the next object on the stack resolves.")
    }
}
