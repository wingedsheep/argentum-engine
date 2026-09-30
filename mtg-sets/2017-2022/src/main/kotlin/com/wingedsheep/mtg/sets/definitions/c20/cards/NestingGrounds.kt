package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Nesting Grounds
 * Land
 *
 * {T}: Add {C}.
 * {1}, {T}: Move a counter from target permanent you control onto a second target permanent.
 * Activate only as a sorcery.
 *
 * The kind of counter is chosen on resolution (Scryfall ruling): `MoveCounterOfAnyKind` asks
 * only when the source carries more than one kind. The second target must differ from the first
 * but may be Nesting Grounds itself, so it doesn't exclude the source.
 */
val NestingGrounds = card("Nesting Grounds") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}: Move a counter from target permanent you control onto a second target permanent. " +
        "Activate only as a sorcery."

    // {T}: Add {C}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {1}, {T}: Move a counter from target permanent you control onto a second target permanent.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val from = target(TargetFilter.PermanentYouControl)
        val onto = target(TargetOther(TargetObject(filter = TargetFilter.Permanent), excludeSource = false))
        effect = Effects.MoveCounterOfAnyKind(source = from, destination = onto)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Yeong-Hao Han"
        flavorText = "Dappled coats become armored hides. Soft jaws become razor tusks. Nestling play becomes the fight for survival."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf559d4e-07a7-4ca4-b76a-d0615222cb60.jpg?1783931205"
    }
}
