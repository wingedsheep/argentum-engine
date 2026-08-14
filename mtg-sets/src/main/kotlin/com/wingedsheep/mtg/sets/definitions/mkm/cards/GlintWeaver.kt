package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Glint Weaver — Murders at Karlov Manor #162
 * {5}{G}{G} · Creature — Spider · 3/3
 *
 * Reach
 * When this creature enters, distribute three +1/+1 counters among one, two, or three target
 * creatures, then you gain life equal to the greatest toughness among creatures you control.
 *
 * Two details the wording is precise about and the script follows literally:
 *
 * - The counters go on "target creatures", **not** "target creatures you control" — an unusual
 *   freedom for a distribute effect, so the target filter is unrestricted. (Handing an opponent
 *   counters is almost never right, but it's legal, and it matters when your own board is empty:
 *   the trigger still needs at least one legal target.)
 * - "then" orders the two halves within one resolution, so the life gain sees the counters already
 *   placed. Weaver's own body is a 3/3, but after distributing all three counters onto it the
 *   greatest toughness among your creatures is 6, not 3.
 *
 * `minCount = 1` encodes "one, two, or three": the distribute helper does the per-target split, and
 * every chosen target must receive at least one counter (CR 601.2d). If all targets become illegal
 * the trigger fizzles and no life is gained; if some remain, the counters redistribute among the
 * legal ones and the life gain still happens.
 */
val GlintWeaver = card("Glint Weaver") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    oracleText = "Reach\n" +
        "When this creature enters, distribute three +1/+1 counters among one, two, or three " +
        "target creatures, then you gain life equal to the greatest toughness among creatures you control."
    power = 3
    toughness = 3
    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetCreature(count = 3, minCount = 1)
        effect = Effects.Composite(
            Effects.DistributeCountersAmongTargets(totalCounters = 3),
            Effects.GainLife(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxToughness()
            )
        )
        description = "When this creature enters, distribute three +1/+1 counters among one, two, " +
            "or three target creatures, then you gain life equal to the greatest toughness among " +
            "creatures you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Tuan Duong Chu"
        flavorText = "The spider just loved how the gems twinkled, and the high-class prey they attracted."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8eff4d0-67ad-4900-b33d-605659b59161.jpg?1783912866"
    }
}
