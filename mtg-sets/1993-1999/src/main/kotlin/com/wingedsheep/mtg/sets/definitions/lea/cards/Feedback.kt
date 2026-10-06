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

val Feedback = card("Feedback") {
    manaCost = "{2}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant enchantment\nAt the beginning of the upkeep of enchanted enchantment's controller, this Aura deals 1 damage to that player."
    colorIdentity = "U"
    auraTarget = TargetObject(filter = TargetFilter.Enchantment)

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0eb8f591-d763-49bf-8ef9-86265aaa72f7.jpg?1783948705"
    }
}
