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

val WarpArtifact = card("Warp Artifact") {
    manaCost = "{B}{B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\nAt the beginning of the upkeep of enchanted artifact's controller, this Aura deals 1 damage to that player."
    colorIdentity = "B"
    auraTarget = TargetObject(filter = TargetFilter.Artifact)

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e5e07a2-fbdf-4c4c-996a-fce40bab5de5.jpg?1783948689"
    }
}
