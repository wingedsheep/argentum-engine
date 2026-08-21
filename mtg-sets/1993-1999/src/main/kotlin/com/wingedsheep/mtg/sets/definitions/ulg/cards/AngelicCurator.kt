package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Angelic Curator — Urza's Legacy #1
 * {1}{W} · Creature — Angel Spirit · 1 / 1
 *
 * Flying, protection from artifacts
 *
 * Urza's Legacy's cheap anti-artifact flier — a sideboard card for a block whose whole theme was
 * artifacts, later reprinted into Modern Horizons 2 (the canonical definition stays here, at the
 * earliest printing; MH2 contributes only a `Printing` row).
 *
 * The two halves take different SDK shapes because protection is parameterised. Flying is a plain
 * enum ([Keyword.FLYING]); protection from artifacts is a [KeywordAbility.Protection] carrying a
 * [ProtectionScope.CardType] of `"Artifact"`, which the engine projects as a scoped protection
 * keyword. All four DEBT consequences (CR 702.16 — can't be Damaged, Enchanted/Equipped, Blocked,
 * or Targeted by artifact sources) fall out of that projection, so nothing further is wired here:
 * an artifact creature can't block it, an artifact's ability can't target it, and damage from an
 * artifact source is prevented.
 *
 * Note this stops *artifact sources*, not artifact creatures specifically, and it does not stop an
 * artifact creature's controller from blocking with it — protection prevents the Curator from
 * being blocked, not from blocking.
 */
val AngelicCurator = card("Angelic Curator") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Spirit"
    power = 1
    toughness = 1
    oracleText = "Flying, protection from artifacts"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.CardType("Artifact")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Greg Staples"
        flavorText = "\"Do not treat your people as you treat your artifacts. Let them go, and they will live; seal them here, and they will die.\"\n—Urza, to Radiant"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c63ba2da-6dea-44ac-8439-527222da565b.jpg?1783946254"
    }
}
