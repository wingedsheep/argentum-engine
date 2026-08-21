package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Thought Monitor — Modern Horizons 2 #71
 * {6}{U} · Artifact Creature — Construct · 2 / 2
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * Flying
 * When this creature enters, draw two cards.
 *
 * Mulldrifter's enters-draw-two body wearing affinity instead of evoke. [KeywordAbility.Affinity]
 * for [CardType.ARTIFACT] is engine-live vocabulary — the cost calculator reads the *KeywordAbility*,
 * never a `Keyword.AFFINITY` enum entry, so the printed keyword line is left for
 * `CardBuilder.build()` to derive; [Keyword.FLYING] is a plain enum keyword and is written out.
 *
 * Affinity shaves generic mana only, so this floors at {U}. The Monitor is itself an artifact, but
 * it counts artifacts *you control* while it is still a spell on the stack, so it never counts
 * itself.
 */
val ThoughtMonitor = card("Thought Monitor") {
    manaCost = "{6}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "Flying\n" +
        "When this creature enters, draw two cards."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(2)
        description = "When this creature enters, draw two cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Martina Pilcerova"
        flavorText = "It roams the skies over the Quicksilver Sea, alert for any hint of aberrant thought."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/996c1952-8d10-4296-8960-ff8993833649.jpg?1783926868"
    }
}
