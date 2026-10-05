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

val Wanderlust = card("Wanderlust") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nAt the beginning of the upkeep of enchanted creature's controller, this Aura deals 1 damage to that player."
    colorIdentity = "G"
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Cornelius Brudi"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/220a03ca-8c9b-4acb-821d-f6577fbb20fb.jpg?1783948671"
    }
}
