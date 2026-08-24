package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Obelisk of Grixis
 * {3}
 * Artifact
 * {T}: Add {U}, {B}, or {R}.
 *
 * One of the Alara shard obelisks. The printed "or" is a choice between three mana abilities, so
 * it is authored as three separate [Effects.AddMana] abilities on [Costs.Tap] — the same shape as
 * the shard tri-lands (cf. Crumbling Necropolis) — each flagged `manaAbility` with
 * [TimingRule.ManaAbility].
 */
val ObeliskOfGrixis = card("Obelisk of Grixis") {
    manaCost = "{3}"
    colorIdentity = "UBR"
    typeLine = "Artifact"
    oracleText = "{T}: Add {U}, {B}, or {R}."

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
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "214"
        artist = "Nils Hamm"
        flavorText = "Like most features of Grixis, the obelisks that remain from the time of Alara now exist only for dark exploitation."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5200e41f-fe10-4c39-ab12-c6e13fb81a3d.jpg"
    }
}
