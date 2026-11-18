package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantProtectionFromChosenColorToGroup
import com.wingedsheep.sdk.scripting.ProtectionSpares
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Flickering Ward
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * As this Aura enters, choose a color.
 * Enchanted creature has protection from the chosen color. This effect
 * doesn't remove this Aura.
 * {W}: Return this Aura to its owner's hand.
 *
 * "This effect doesn't remove this Aura" is `spares = ProtectionSpares.SELF`: the
 * chosen-color protection removes other Auras of that color (CR 704.5m) but not
 * this one.
 */
val FlickeringWard = card("Flickering Ward") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "As this Aura enters, choose a color.\n" +
        "Enchanted creature has protection from the chosen color. This effect doesn't remove this Aura.\n" +
        "{W}: Return this Aura to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    staticAbility {
        ability = GrantProtectionFromChosenColorToGroup(
            filter = GroupFilter.attachedCreature(),
            spares = ProtectionSpares.SELF
        )
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Greg Simanson"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4d2b011-bb0d-463c-bf2a-04b6650771a3.jpg?1562056865"
    }
}
