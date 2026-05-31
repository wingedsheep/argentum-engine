package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Krumar Initiate — Tarkir: Dragonstorm #84
 * {1}{B} · Creature — Human Cleric · 2/2
 *
 * {X}{B}, {T}, Pay X life: This creature endures X. Activate only as a sorcery.
 * (Put X +1/+1 counters on it or create an X/X white Spirit creature token.)
 *
 * The {X} mana cost drives X; [Costs.PayXLife] consumes the same chosen X in life,
 * and the endure amount reads [DynamicAmount.XValue]. The activation is gated to
 * sorcery speed via [TimingRule.SorcerySpeed].
 */
val KrumarInitiate = card("Krumar Initiate") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "{X}{B}, {T}, Pay X life: This creature endures X. Activate only as a sorcery. " +
        "(Put X +1/+1 counters on it or create an X/X white Spirit creature token.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{B}"), Costs.Tap, Costs.PayXLife)
        timing = TimingRule.SorcerySpeed
        effect = Effects.Endure(DynamicAmount.XValue)
        description = "{X}{B}, {T}, Pay X life: This creature endures X. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Josu Solano"
        flavorText = "\"Welcome,\" the spirits of House Emesh whispered to Cemal. \"Welcome, newest sprout of our Kin-Tree.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc66680f-24ab-433a-8197-feac3a174075.jpg?1743204296"
    }
}
