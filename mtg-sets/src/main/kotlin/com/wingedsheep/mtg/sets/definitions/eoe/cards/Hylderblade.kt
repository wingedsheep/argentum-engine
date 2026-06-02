package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hylderblade
 * {B}
 * Artifact — Equipment
 * Equipped creature gets +3/+1.
 * Void — At the beginning of your end step, if a nonland permanent left the battlefield this turn
 *   or a spell was warped this turn, attach this Equipment to target creature you control.
 * Equip {4}
 */
val Hylderblade = card("Hylderblade") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+1.\nVoid — At the beginning of your end step, if a nonland permanent left the battlefield this turn or a spell was warped this turn, attach this Equipment to target creature you control.\nEquip {4}"

    staticAbility {
        ability = ModifyStats(3, 1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.Void
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = AttachEquipmentEffect(target = creature)
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac80cf18-0707-4358-bdd4-0c2b90d0a1d9.jpg?1752946985"
    }
}
