package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tenured Concocter — Secrets of Strixhaven #163
 * {4}{G} · Creature — Troll Druid · 4/5
 *
 * Vigilance
 * Whenever this creature becomes the target of a spell or ability an opponent controls,
 * you may draw a card.
 * Infusion — This creature gets +2/+0 as long as you gained life this turn.
 *
 * The targeting trigger is the self-bound [Triggers.BecomesTargetByOpponent] with
 * `optional = true` for the "you may" draw (Cactarantula idiom). Infusion is an ability
 * word (no rules meaning, CR 207.2c) — the +2/+0 is a conditional self-buff static gated
 * on [Conditions.YouGainedLifeThisTurn], scoped to this creature via [GroupFilter.source].
 */
val TenuredConcocter = card("Tenured Concocter") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll Druid"
    power = 4
    toughness = 5
    oracleText = "Vigilance\n" +
        "Whenever this creature becomes the target of a spell or ability an opponent " +
        "controls, you may draw a card.\n" +
        "Infusion — This creature gets +2/+0 as long as you gained life this turn."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.BecomesTargetByOpponent
        optional = true
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        condition = Conditions.YouGainedLifeThisTurn
        ability = ModifyStats(powerBonus = 2, toughnessBonus = 0, filter = GroupFilter.source())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Lie Setiawan"
        flavorText = "\"Potential can be augmented. Imagination cannot.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/376c8b7d-1c90-47e1-bd01-e4c67f3fc4fc.jpg?1775938116"
    }
}
