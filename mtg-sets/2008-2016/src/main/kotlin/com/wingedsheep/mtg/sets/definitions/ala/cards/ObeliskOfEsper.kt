package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Obelisk of Esper
 * {3}
 * Artifact
 * {T}: Add {W}, {U}, or {B}.
 *
 * One of the Alara shard obelisks. The printed "or" is a choice between three mana abilities, so
 * it is authored as three separate [Effects.AddMana] abilities on [Costs.Tap] — the same shape as
 * the shard tri-lands (cf. Arcane Sanctum) — each flagged `manaAbility` with
 * [TimingRule.ManaAbility].
 */
val ObeliskOfEsper = card("Obelisk of Esper") {
    manaCost = "{3}"
    colorIdentity = "WUB"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W}, {U}, or {B}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "213"
        artist = "Francis Tsai"
        flavorText = "It is a monument as austere, unyielding, and inscrutable as Esper itself."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19869e75-0925-45dc-b5d9-6968fbfb35b5.jpg"
    }
}
