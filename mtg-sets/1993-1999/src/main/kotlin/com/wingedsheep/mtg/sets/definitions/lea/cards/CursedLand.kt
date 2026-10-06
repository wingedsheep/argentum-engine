package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

val CursedLand = card("Cursed Land") {
    manaCost = "{2}{B}{B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nAt the beginning of the upkeep of enchanted land's controller, this Aura deals 1 damage to that player."
    colorIdentity = "B"
    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf5f3c61-1e54-4eea-bf82-311cfa988e6a.jpg?1783948697"
    }
}
