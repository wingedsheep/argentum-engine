package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Starnheim Memento — Foundations Jumpstart #29
 * {3} · Artifact
 *
 * {T}: Add {W}.
 * {1}{W}, {T}: Target creature gets +1/+1 and gains flying until end of turn. Activate only as a sorcery.
 */
val StarnheimMemento = card("Starnheim Memento") {
    manaCost = "{3}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W}.\n" +
        "{1}{W}, {T}: Target creature gets +1/+1 and gains flying until end of turn. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, t) then Effects.GrantKeyword(Keyword.FLYING, t)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Steve Ellis"
        flavorText = "Made from a feather from Firja's wing, it marks a worthy hero with the blessing of the valkyries."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16fe39b7-0dbe-4826-aa0a-59e93c6aaa01.jpg?1783908861"
    }
}
