package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nightdrinker Moroii — Murders at Karlov Manor #96
 * {3}{B} · Creature — Vampire · 4/2
 *
 * Flying
 * When this creature enters, you lose 3 life.
 * Disguise {B}{B}
 *
 * The plain disguise shape: nothing on the card knows about the mechanic beyond the `disguise`
 * cost. Casting it face down for {3} produces a 2/2 with ward {2} whose characteristics come
 * entirely from CR 702.168a, so the flying and the enters trigger are both suppressed (CR 708.2)
 * until it's turned face up — and per CR 702.168d turning it face up is *not* entering the
 * battlefield, so the "you lose 3 life" never fires that way. Paying {3} to dodge the life loss is
 * the whole point of the card.
 */
val NightdrinkerMoroii = card("Nightdrinker Moroii") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Flying\n" +
        "When this creature enters, you lose 3 life.\n" +
        "Disguise {B}{B} (You may cast this card face down for {3} as a 2/2 creature with ward {2}. " +
        "Turn it face up any time for its disguise cost.)"
    power = 4
    toughness = 2
    keywords(Keyword.FLYING)
    disguise = "{B}{B}"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.You))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce043cba-aea4-4156-b1d0-545eda06c400.jpg"
    }
}
