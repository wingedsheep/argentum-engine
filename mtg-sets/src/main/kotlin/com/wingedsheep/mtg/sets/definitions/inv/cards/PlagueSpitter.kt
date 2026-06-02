package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Plague Spitter
 * {2}{B}
 * Creature — Phyrexian Horror
 * 2/2
 * At the beginning of your upkeep, this creature deals 1 damage to each creature and each player.
 * When this creature dies, it deals 1 damage to each creature and each player.
 */
val PlagueSpitter = card("Plague Spitter") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Horror"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your upkeep, this creature deals 1 damage to each creature and each player.\n" +
        "When this creature dies, it deals 1 damage to each creature and each player."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.ForEachInGroup(GroupFilter.AllCreatures, DealDamageEffect(1, EffectTarget.Self)) then
            Effects.DealDamage(1, EffectTarget.PlayerRef(Player.Each))
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.ForEachInGroup(GroupFilter.AllCreatures, DealDamageEffect(1, EffectTarget.Self)) then
            Effects.DealDamage(1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Chippy"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8845e6bd-40ee-45ca-a099-53f19ff20a8a.jpg?1562922550"
    }
}
