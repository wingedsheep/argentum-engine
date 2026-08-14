package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Broodstar — Mirrodin #31
 * {8}{U}{U} · Creature — Beast · star/star
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * Flying
 * Broodstar's power and toughness are each equal to the number of artifacts you control.
 *
 * The affinity top-end: eight artifacts makes it {U}{U} for an 8/8 flier. Affinity only shaves
 * generic mana, so the {U}{U} is never reduced away.
 *
 * `dynamicStats` is the characteristic-defining ability (CR 604.3) behind the printed star/star:
 * base power and toughness are both set in Layer 7b from the count of artifacts you control, re-read
 * continuously rather than snapshotted. Broodstar is not itself an artifact, so it never counts
 * toward its own size — a lone Broodstar on an empty board is 0/0 and dies to the SBA.
 */
val Broodstar = card("Broodstar") {
    manaCost = "{8}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Beast"
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "Flying\n" +
        "Broodstar's power and toughness are each equal to the number of artifacts you control."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))
    keywords(Keyword.FLYING)

    dynamicStats(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count())

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07a194cb-53c9-4690-ba63-79beecaebe0e.jpg?1783944557"
    }
}
