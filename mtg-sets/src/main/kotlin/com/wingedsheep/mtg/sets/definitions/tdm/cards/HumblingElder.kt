package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Humbling Elder — Tarkir: Dragonstorm #48
 * {U} · Creature — Human Monk · 1/2
 *
 * Flash
 * When this creature enters, target creature an opponent controls gets -2/-0
 * until end of turn.
 */
val HumblingElder = card("Humbling Elder") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 2
    oracleText = "Flash\n" +
        "When this creature enters, target creature an opponent controls gets -2/-0 until end of turn."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature", Targets.CreatureOpponentControls)
        effect = Effects.ModifyStats(-2, 0, creature)
        description = "When this creature enters, target creature an opponent controls gets -2/-0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Yohann Schepacz"
        flavorText = "\"There is no better way to measure a person's true character than to let them think they're above you.\"\n—Master Xian"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a84c3f8-0030-4653-880e-b2d19272f5fa.jpg?1743697602"
    }
}
