package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shadow Alley Denizen
 * {B}
 * Creature — Vampire Rogue
 * 1/1
 * Whenever another black creature you control enters, target creature gains intimidate until end of turn.
 */
val ShadowAlleyDenizen = card("Shadow Alley Denizen") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "Whenever another black creature you control enters, target creature gains intimidate until " +
        "end of turn. (It can't be blocked except by artifact creatures and/or creatures that share a color with it.)"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withColor(Color.BLACK).youControl()).enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.INTIMIDATE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/985997ae-59bc-49d7-87ca-e63ed9706fdf.jpg"
    }
}
