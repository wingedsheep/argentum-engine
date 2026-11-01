package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tenth District Veteran — Ravnica Allegiance #26
 * {2}{W} · Creature — Human Soldier · 2 / 3
 */
val TenthDistrictVeteran = card("Tenth District Veteran") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Vigilance\nWhenever this creature attacks, untap another target creature you control."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Volkan Baǵa"
        flavorText = "\"I keep reminding myself we do this because others can't, because we love this city. " +
            "If we don't save it, no one will.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef573e92-3106-4b42-90e8-0e165de0659f.jpg?1783933715"
    }
}
