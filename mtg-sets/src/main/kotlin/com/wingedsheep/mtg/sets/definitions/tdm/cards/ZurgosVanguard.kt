package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mobilize
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Zurgo's Vanguard — Tarkir: Dragonstorm #133
 * {2}{R} · Creature — Dog Soldier · star/3 (power is a characteristic-defining count)
 *
 * Mobilize 1 (Whenever this creature attacks, create a tapped and attacking 1/1 red Warrior
 * creature token. Sacrifice it at the beginning of the next end step.)
 * Zurgo's Vanguard's power is equal to the number of creatures you control.
 *
 * `mobilize(1)` supplies the attack-triggered Warrior token. The characteristic-defining power is a
 * power-only `dynamicPower(...)` over [DynamicAmount.AggregateBattlefield] counting
 * creatures you control (toughness stays fixed at 3). Because Mobilize's token enters tapped and
 * attacking before damage, the Warrior it makes is itself a creature you control, so the
 * count-based power already reflects the new token when combat damage is assigned.
 */
val ZurgosVanguard = card("Zurgo's Vanguard") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dog Soldier"
    toughness = 3
    oracleText = "Mobilize 1 (Whenever this creature attacks, create a tapped and attacking 1/1 red " +
        "Warrior creature token. Sacrifice it at the beginning of the next end step.)\n" +
        "Zurgo's Vanguard's power is equal to the number of creatures you control."

    mobilize(1)
    dynamicPower(
        DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature)
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1aa3501-5738-4063-a7f4-51d2600b0041.jpg?1743204499"
    }
}
