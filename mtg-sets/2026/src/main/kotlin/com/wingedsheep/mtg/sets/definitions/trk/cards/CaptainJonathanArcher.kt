package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Captain Jonathan Archer
 * {2}{G}
 * Legendary Creature — Human Officer
 * 3/3
 *
 * When Captain Archer enters, create Porthos, a legendary 1/1 green Dog creature token.
 * Federation — {G}, {T}: Target creature you control gets +X/+X until end of turn, where X is the
 * number of creature types among non-Borg creatures you control. Activate only as a sorcery.
 *
 * X is locked in as the ability resolves (`DynamicAmounts.federation()`).
 */
val CaptainJonathanArcher = card("Captain Jonathan Archer") {
    manaCost = "{2}{G}"
    typeLine = "Legendary Creature — Human Officer"
    colorIdentity = "G"
    oracleText = "When Captain Archer enters, create Porthos, a legendary 1/1 green Dog creature token.\n" +
        "Federation — {G}, {T}: Target creature you control gets +X/+X until end of turn, where X is the " +
        "number of creature types among non-Borg creatures you control. Activate only as a sorcery."
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Dog"),
            name = "Porthos",
            legendary = true,
        )
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(DynamicAmounts.federation(), DynamicAmounts.federation(), creature)
        timing = TimingRule.SorcerySpeed
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4518ea3-5f56-4f1d-9024-2bf06ac98786.jpg?1792508262"
    }
}
