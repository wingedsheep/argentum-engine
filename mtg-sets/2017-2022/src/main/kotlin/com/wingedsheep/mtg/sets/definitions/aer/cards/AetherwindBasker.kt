package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aetherwind Basker
 * {4}{G}{G}{G}
 * Creature — Lizard
 * 7/7
 * Trample
 * Whenever this creature enters or attacks, you get {E} (an energy counter) for each creature you control.
 * Pay {E}: This creature gets +1/+1 until end of turn.
 *
 * - "Enters **or** attacks" is two triggered abilities sharing one effect (the Grave Titan shape).
 * - The count is taken on resolution and includes the Basker itself ("each creature you control").
 */
val AetherwindBasker = card("Aetherwind Basker") {
    manaCost = "{4}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Lizard"
    power = 7
    toughness = 7
    oracleText = "Trample\n" +
        "Whenever this creature enters or attacks, you get {E} (an energy counter) for each creature you control.\n" +
        "Pay {E}: This creature gets +1/+1 until end of turn."

    keywords(Keyword.TRAMPLE)

    val energyDescription = "Whenever this creature enters or attacks, you get {E} for each creature you control."
    val creaturesYouControl = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count()

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddDynamicCounters(CounterType.ENERGY, creaturesYouControl, EffectTarget.Controller)
        description = energyDescription
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.AddDynamicCounters(CounterType.ENERGY, creaturesYouControl, EffectTarget.Controller)
        description = energyDescription
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 1)
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Pay {E}: This creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "104"
        artist = "Svetlin Velinov"
        flavorText = "Its frill inspired the design of efficient aether collectors."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa04d5fa-6ba2-4833-8ea0-1941a03bd3e9.jpg?1783936746"
        ruling(
            "2017-02-09",
            "Abilities that trigger \"whenever you get one or more {E},\" such as that of Fabrication Module, " +
                "trigger only once as Aetherwind Basker's triggered ability resolves."
        )
        ruling(
            "2017-02-09",
            "The triggered ability triggers both when Aetherwind Basker enters the battlefield and whenever it " +
                "attacks. You don't have to choose only one."
        )
    }
}
