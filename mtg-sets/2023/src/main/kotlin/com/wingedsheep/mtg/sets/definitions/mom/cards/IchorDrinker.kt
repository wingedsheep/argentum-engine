package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Ichor Drinker
 * {B}
 * Creature — Phyrexian Vampire
 * 1/1
 * Lifelink
 * {B}, Exile this card from your graveyard: Incubate 2. Activate only as a sorcery.
 */
val IchorDrinker = card("Ichor Drinker") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Vampire"
    power = 1
    toughness = 1
    oracleText = "Lifelink\n{B}, Exile this card from your graveyard: Incubate 2. Activate only as a sorcery. " +
        "(Create an Incubator token with two +1/+1 counters on it and \"{2}: Transform this token.\" " +
        "It transforms into a 0/0 Phyrexian artifact creature.)"

    keywords(Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.ExileSelf)
        effect = Effects.Incubate(2)
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Aurore Folny"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91fd7350-cc97-4c35-a072-2c826984293c.jpg?1783917006"
    }
}
