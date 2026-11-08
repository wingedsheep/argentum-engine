package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Servant of the Conduit — Kaladesh #169
 * {1}{G} · Creature — Elf Druid · 2/2
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * {T}, Pay {E}: Add one mana of any color.
 *
 * Same energy mana ability shape as Solar Transformer.
 */
val ServantOfTheConduit = card("Servant of the Conduit") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "{T}, Pay {E}: Add one mana of any color."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 1))
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Magali Villeneuve"
        flavorText = "\"Creation without connection to nature is just manufacture.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9ce9cf30-4baa-4f28-84e8-b9b1168a40e0.jpg?1783937173"
    }
}
