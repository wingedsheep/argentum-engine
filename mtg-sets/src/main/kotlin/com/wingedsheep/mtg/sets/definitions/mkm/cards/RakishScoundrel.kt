package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rakish Scoundrel — Murders at Karlov Manor #225
 * {2}{B}{G} · Creature — Elf Rogue · 3/3
 *
 * Deathtouch
 * When this creature enters or is turned face up, target creature gains indestructible until end
 * of turn.
 * Disguise {4}{B/G}{B/G}
 *
 * One ability with two trigger conditions, so it's one `Triggers.or` of the two SELF-bound event
 * patterns rather than two abilities — which matters: it must fire exactly once on either route,
 * never twice. The two routes are genuinely disjoint (CR 702.168d: turning face up is not entering
 * the battlefield), so a disguised copy gets the indestructible when it flips and a hard-cast one
 * gets it on entry.
 */
val RakishScoundrel = card("Rakish Scoundrel") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Rogue"
    oracleText = "Deathtouch\n" +
        "When this creature enters or is turned face up, target creature gains indestructible until end of turn.\n" +
        "Disguise {4}{B/G}{B/G} (You may cast this card face down for {3} as a 2/2 creature with ward {2}. " +
        "Turn it face up any time for its disguise cost.)"
    power = 3
    toughness = 3
    keywords(Keyword.DEATHTOUCH)
    disguise = "{4}{B/G}{B/G}"

    triggeredAbility {
        trigger = Triggers.or(Triggers.EntersBattlefield, Triggers.TurnedFaceUp)
        val creature = target("target creature", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Ina Wong"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6aaa8c6b-7ef7-45db-99c9-4a6e7f177b94.jpg"
    }
}
