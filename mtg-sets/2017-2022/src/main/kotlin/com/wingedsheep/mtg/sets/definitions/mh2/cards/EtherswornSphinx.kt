package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Ethersworn Sphinx — Modern Horizons 2 #195
 * {7}{W}{U} · Artifact Creature — Sphinx · 4 / 4
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * Flying
 * Cascade (When you cast this spell, exile cards from the top of your library until you exile a
 * nonland card with lesser mana value. You may cast it without paying its mana cost. Put the exiled
 * cards on the bottom in a random order.)
 *
 * Two keywords, lowered two different ways because the engine reads them differently.
 *
 * **Affinity** is engine-live, but only through the *ability*: the cost calculator inspects
 * [KeywordAbility.Affinity], never `Keyword.AFFINITY`, so the ability is what gets written (the
 * display keyword is derived from it). Affinity reduces generic mana only, so the {W}{U} pips floor
 * the cost at two mana however many artifacts are out, and the Sphinx's mana value stays 9 — which
 * is exactly what makes cascade here so large. Same shape as `mh3/cards/FurnaceHellkite.kt`.
 *
 * **Cascade** is not: [Keyword.CASCADE] is display-only vocabulary and nothing in the rules engine
 * reads it. Cascade *is* a "when you cast this spell" triggered ability (CR 702.85a), so the
 * behaviour lives in a [Triggers.WhenYouCastThisSpell] trigger feeding [Effects.Cascade], with the
 * keyword kept only for the printed line — the canonical lowering in `arb/cards/BloodbraidElf.kt`.
 * Note the two do not interact: cascade compares against the *printed* mana value, so affinity
 * making the Sphinx cheap to cast never shrinks what cascade can hit.
 */
val EtherswornSphinx = card("Ethersworn Sphinx") {
    manaCost = "{7}{W}{U}"
    colorIdentity = "UW"
    typeLine = "Artifact Creature — Sphinx"
    power = 4
    toughness = 4
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "Flying\n" +
        "Cascade (When you cast this spell, exile cards from the top of your library until you exile a nonland card with lesser mana value. You may cast it without paying its mana cost. Put the exiled cards on the bottom in a random order.)"

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))
    keywords(Keyword.FLYING, Keyword.CASCADE)

    // Cascade — the cast trigger the keyword abbreviates (CR 702.85a).
    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        effect = Effects.Cascade
        description = "Cascade"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/315ae21a-4d95-488e-812b-0d018219af6c.jpg?1783926817"
    }
}
