package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soul Collector
 * {3}{B}{B}
 * Creature — Vampire
 * 3/4
 * Flying
 * Whenever a creature dealt damage by Soul Collector this turn dies,
 * return that card to the battlefield under your control.
 * Morph {B}{B}{B}
 */
val SoulCollector = card("Soul Collector") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhenever a creature dealt damage by Soul Collector this turn dies, return that card to the battlefield under your control.\nMorph {B}{B}{B}"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.CreatureDealtDamageByThisDies
        effect = Effects.PutOntoBattlefieldUnderYourControl(EffectTarget.TriggeringEntity)
    }

    morph = "{B}{B}{B}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Matthew D. Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec78c0e8-a354-46d2-95ad-012f120c3df8.jpg?1562536662"
        ruling("2004-10-04", "The ability only triggers if this card is face up at the time the creature is put into the graveyard.")
    }
}
