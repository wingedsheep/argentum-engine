package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reflections of Littjara
 * {4}{U}
 * Enchantment
 * As this enchantment enters, choose a creature type.
 * Whenever you cast a spell of the chosen type, copy that spell. (A copy of a permanent spell becomes a token.)
 *
 * No "you may choose new targets": a copied Kindred instant or Aura keeps the original's targets.
 */
val ReflectionsOfLittjara = card("Reflections of Littjara") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a creature type.\n" +
        "Whenever you cast a spell of the chosen type, copy that spell. (A copy of a permanent spell becomes a token.)"

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withChosenSubtype())
        effect = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity, mayChooseNewTargets = false)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Aaron Miller"
        flavorText = "The masks float downstream toward the Mirrorlake, where faces arise to wear them."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eadee447-ce9a-4dea-a074-bfd04197548e.jpg?1783928257"
    }
}
