package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Daily Bugle Building
 * Land
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color.
 * Smear Campaign — {1}, {T}: Target legendary creature gains menace until end of turn. Activate only as a sorcery.
 */
val DailyBugleBuilding = card("Daily Bugle Building") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{1}, {T}: Add one mana of any color.\nSmear Campaign — {1}, {T}: Target legendary creature gains menace until end of turn. Activate only as a sorcery."
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.legendary()))
        effect = Effects.GrantKeyword(Keyword.MENACE, t)
        timing = TimingRule.SorcerySpeed
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "David Álvarez"
        flavorText = "Known as \"New York's Finest Newspaper,\" the *Daily Bugle* takes pride in always being fair and impartial."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/669bbcb1-0981-40e7-905e-b94e74bc4861.jpg?1757378130"
    }
}
