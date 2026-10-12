package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Meria's Outrider
 * {4}{R}
 * Creature — Elf Archer
 * 4/4
 * Reach
 * Domain — When this creature enters, it deals damage to each opponent equal to the number of
 * basic land types among lands you control.
 */
val MeriasOutrider = card("Meria's Outrider") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elf Archer"
    power = 4
    toughness = 4
    oracleText = "Reach\nDomain — When this creature enters, it deals damage to each opponent equal to the number of basic land types among lands you control."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(DynamicAmounts.domain(), EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Lius Lasahido"
        flavorText = "Arrows fashioned from the living wood of Yavimaya carry all of the forest's rage in their fire-hardened points."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48aa964f-99b5-4b6b-b158-42f39514c910.jpg?1783921313"
    }
}
