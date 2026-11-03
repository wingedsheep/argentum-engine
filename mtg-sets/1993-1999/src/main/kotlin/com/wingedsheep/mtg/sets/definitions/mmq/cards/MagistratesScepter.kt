package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Magistrate's Scepter — Mercadian Masques #304
 * {3} · Artifact
 *
 * {4}, {T}: Put a charge counter on this artifact.
 * {T}, Remove three charge counters from this artifact: Take an extra turn after this one.
 *
 * Two plain activated abilities: a mana-plus-tap cost that banks one charge counter, and a
 * tap-plus-remove-counters cost (the Mirrodin's Core shape) that cashes three in for an extra turn
 * (the Time Vault effect).
 */
val MagistratesScepter = card("Magistrate's Scepter") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{4}, {T}: Put a charge counter on this artifact.\n" +
        "{T}, Remove three charge counters from this artifact: Take an extra turn after this one."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }
    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.CHARGE, 3)
        )
        effect = Effects.TakeExtraTurn()
        description = "Take an extra turn"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "304"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4785ed7-c948-4ad2-b24d-2f45806d9fcc.jpg?1783945912"
        ruling("2018-07-13", "If multiple extra turns would be taken after this one, perhaps because more than one player has activated the last ability of their Magistrate's Scepter, the most recently created one is taken first.")
    }
}
