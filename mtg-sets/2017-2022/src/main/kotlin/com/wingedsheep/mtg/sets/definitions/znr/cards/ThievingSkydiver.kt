package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thieving Skydiver
 * {1}{U}
 * Creature — Merfolk Rogue
 * 2/1
 * Kicker {X}. X can't be 0. (You may pay an additional {X} as you cast this spell.)
 * Flying
 * When this creature enters, if it was kicked, gain control of target artifact with mana value X
 * or less. If that artifact is an Equipment, attach it to this creature.
 *
 * The kicked cast floors X at 1 (`kicker("{X}", minimumXValue = 1)`); the X chosen flows onto the
 * enters event, so the trigger's target filter reads it via `manaValueAtMostX()`.
 */
val ThievingSkydiver = card("Thieving Skydiver") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    power = 2
    toughness = 1
    oracleText = "Kicker {X}. X can't be 0. (You may pay an additional {X} as you cast this spell.)\n" +
        "Flying\n" +
        "When this creature enters, if it was kicked, gain control of target artifact with mana value X or less. " +
        "If that artifact is an Equipment, attach it to this creature."

    keywordAbility(KeywordAbility.kicker("{X}", minimumXValue = 1))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.manaValueAtMostX()))
        effect = Effects.GainControl(artifact) then
            Effects.If(
                condition = Conditions.EntityMatches(artifact, GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)),
                then = Effects.AttachTargetEquipmentToCreature(equipmentTarget = artifact, creatureTarget = EffectTarget.Self)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff84ea71-e477-44f7-a3f8-77fef708efeb.jpg?1783929385"
    }
}
