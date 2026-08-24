package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Obelisk of Jund
 * {3}
 * Artifact
 * {T}: Add {B}, {R}, or {G}.
 *
 * One of the Alara shard obelisks. The printed "or" is a choice between three mana abilities, so
 * it is authored as three separate [Effects.AddMana] abilities on [Costs.Tap] — the same shape as
 * the shard tri-lands (cf. Savage Lands) — each flagged `manaAbility` with
 * [TimingRule.ManaAbility].
 */
val ObeliskOfJund = card("Obelisk of Jund") {
    manaCost = "{3}"
    colorIdentity = "BRG"
    typeLine = "Artifact"
    oracleText = "{T}: Add {B}, {R}, or {G}."

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
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "215"
        artist = "Brandon Kitkouski"
        flavorText = "Volcanic ash-winds batter it, climbing vines overwhelm it, dragonfire roasts it, and yet it still stands as a testament to a forgotten world."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffe1a49a-5c1c-4274-a2ed-9a8a44abfaa6.jpg"
    }
}
