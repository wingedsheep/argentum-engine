package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hightide Hermit — Kaladesh #51
 * {4}{U} · Creature — Crab · 4/4
 *
 * Defender
 * When this creature enters, you get {E}{E}{E}{E} (four energy counters).
 * Pay {E}{E}: This creature can attack this turn as though it didn't have defender.
 *
 * Consulate Surveillance's energy shape with Roving Keep's defender-lift effect.
 */
val HightideHermit = card("Hightide Hermit") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Crab"
    power = 4
    toughness = 4
    oracleText = "Defender\n" +
        "When this creature enters, you get {E}{E}{E}{E} (four energy counters).\n" +
        "Pay {E}{E}: This creature can attack this turn as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(4)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 2)
        effect = Effects.CanAttackDespiteDefenderThisTurn()
        description = "Pay {E}{E}: This creature can attack this turn as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Jason Kang"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69c871fa-d333-4c73-9d5c-f5cfec5954da.jpg?1783937220"
    }
}
