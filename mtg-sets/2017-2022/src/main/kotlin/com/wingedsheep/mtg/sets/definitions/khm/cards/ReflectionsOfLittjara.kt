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
 *
 * As this enchantment enters, choose a creature type.
 * Whenever you cast a spell of the chosen type, copy that spell. (A copy of a permanent spell
 * becomes a token.)
 *
 * "A spell of the chosen type" is any spell carrying that creature type — a creature or a kindred
 * spell alike — read through `withChosenSubtype()` against the type chosen as this entered. The
 * printed text grants no "you may choose new targets", so the copy keeps the original's targets
 * (`mayChooseNewTargets = false`).
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
        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity, mayChooseNewTargets = false)
        description = "Whenever you cast a spell of the chosen type, copy that spell."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Aaron Miller"
        flavorText = "The masks float downstream toward the Mirrorlake, where faces arise to wear them."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eadee447-ce9a-4dea-a074-bfd04197548e.jpg?1783928257"
        ruling("2021-02-05", "You must choose an existing creature type, such as Zombie or Angel. You can't choose card types (e.g., artifact) or supertypes (e.g., snow).")
        ruling("2021-02-05", "The triggered ability and the copy it creates will resolve before the spell that caused the ability to trigger.")
        ruling("2021-02-05", "As a copy of a permanent spell resolves, it's put onto the battlefield as a token rather than putting a copy of the spell onto the battlefield. The rules that apply to a permanent spell becoming a permanent apply to a copy of a spell becoming a token.")
        ruling("2021-02-05", "The token that a resolving copy of a permanent spell becomes isn't \"created.\" Abilities that refer to a copy being created won't interact with the copy resolving.")
        ruling("2021-02-05", "If a spell has the kindred card type and the chosen creature type, Reflections of Littjara will copy it.")
    }
}
