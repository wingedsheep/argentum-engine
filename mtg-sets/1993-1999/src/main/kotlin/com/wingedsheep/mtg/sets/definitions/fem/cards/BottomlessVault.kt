package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bottomless Vault
 * Land
 * This land enters tapped.
 * You may choose not to untap this land during your untap step.
 * At the beginning of your upkeep, if this land is tapped, put a storage counter on it.
 * {T}, Remove any number of storage counters from this land: Add {B} for each storage counter
 * removed this way.
 *
 * The storage-land cycle. Three pieces make it work: the optional-untap flag (CR 502.3) lets the
 * controller leave it tapped, the upkeep trigger's *intervening if* (CR 603.4) only charges it
 * while it is tapped, and the mana ability's variable counter-removal cost feeds its own
 * [DynamicAmount.XValue] back into the amount of mana produced.
 */
val BottomlessVault = card("Bottomless Vault") {
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "You may choose not to untap this land during your untap step.\n" +
        "At the beginning of your upkeep, if this land is tapped, put a storage counter on it.\n" +
        "{T}, Remove any number of storage counters from this land: Add {B} for each storage counter removed this way."

    replacementEffect(EntersTapped())

    flags(AbilityFlag.MAY_NOT_UNTAP)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        interveningIf = Conditions.SourceIsTapped
        effect = Effects.AddCounters(Counters.STORAGE, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, if this land is tapped, put a storage counter on it."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveXCounters(counterType = Counters.STORAGE, self = true),
        )
        manaAbility = true
        effect = Effects.AddMana(Color.BLACK, DynamicAmount.XValue)
        description = "{T}, Remove any number of storage counters from this land: Add {B} for each storage counter removed this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Pat Lewis"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/639ae988-d1d1-4ead-b0f8-47fc39eb64a0.jpg?1783947879"
    }
}
