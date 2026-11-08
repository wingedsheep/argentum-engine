package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lightning Runner
 * {3}{R}{R}
 * Creature — Human Warrior
 * 2/2
 * Double strike, haste
 * Whenever this creature attacks, you get {E}{E} (two energy counters), then you may pay eight {E}.
 * If you pay, untap all creatures you control, and after this phase, there is an additional combat phase.
 *
 * The added phase is combat only — no trailing main phase — which is the [Effects.AddCombatPhase] atom.
 */
val LightningRunner = card("Lightning Runner") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Double strike, haste\n" +
        "Whenever this creature attacks, you get {E}{E} (two energy counters), then you may pay eight {E}. " +
        "If you pay, untap all creatures you control, and after this phase, there is an additional combat phase."

    keywords(Keyword.DOUBLE_STRIKE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.GetEnergy(2) then
            Effects.MayPay(
                cost = Effects.PayExactCounters(CounterType.ENERGY, 8),
                then = Patterns.Group.untapGroup(GroupFilter.AllCreaturesYouControl) then
                    Effects.AddCombatPhase,
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "90"
        artist = "Raymond Swanland"
        flavorText = "\"Catch me if you can.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5d9c7eb-dca4-4471-a91f-a5aad3a69c2f.jpg?1783936753"
        ruling("2017-02-09", "You untap all creatures you control, including ones that aren't attacking.")
    }
}
