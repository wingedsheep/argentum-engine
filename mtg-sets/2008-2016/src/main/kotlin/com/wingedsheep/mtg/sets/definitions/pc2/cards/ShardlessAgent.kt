package com.wingedsheep.mtg.sets.definitions.pc2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shardless Agent — Planechase 2012 #104
 * {1}{G}{U} · Artifact Creature — Human Rogue · 2 / 2
 *
 * Cascade (When you cast this spell, exile cards from the top of your library until you exile a
 * nonland card that costs less. You may cast it without paying its mana cost. Put the exiled cards
 * on the bottom in a random order.)
 *
 * Planechase 2012 is the Agent's earliest real printing, so the canonical definition lives here;
 * the Modern Horizons 2 appearance is a `Printing` row (`mh2/cards/ShardlessAgentReprint.kt`).
 *
 * [Keyword.CASCADE] is display-only vocabulary — nothing in the rules engine reads it. Cascade *is*
 * a "when you cast this spell" triggered ability (CR 702.85a), so the behaviour lives in a
 * [Triggers.WhenYouCastThisSpell] trigger feeding [Effects.Cascade], with the keyword kept only for
 * the printed line — the canonical lowering in `arb/cards/BloodbraidElf.kt`. The trigger goes on the
 * stack above the Agent itself, so the free spell resolves first and the 2/2 body lands after it.
 */
val ShardlessAgent = card("Shardless Agent") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Artifact Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "Cascade (When you cast this spell, exile cards from the top of your library until you exile a nonland card that costs less. You may cast it without paying its mana cost. Put the exiled cards on the bottom in a random order.)"

    keywords(Keyword.CASCADE)

    // Cascade — the cast trigger the keyword abbreviates (CR 702.85a).
    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        effect = Effects.Cascade
        description = "Cascade"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ceb4df89-a97e-4479-b7f0-7083417a9565.jpg?1783940595"
    }
}
