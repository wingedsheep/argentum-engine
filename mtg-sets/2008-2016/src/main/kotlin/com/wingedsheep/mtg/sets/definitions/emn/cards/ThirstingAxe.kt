package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.DamageRecipientKind
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thirsting Axe
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +4/+0.
 * At the beginning of your end step, if equipped creature didn't deal combat damage to a creature
 * this turn, sacrifice it.
 * Equip {2}
 *
 * "It" is the equipped creature. The intervening-if asks about whichever creature the Axe is attached
 * to as the end step begins, and it doesn't matter whether the Axe was attached when the damage was
 * dealt (rulings); combat damage only to a player or planeswalker doesn't count. With nothing
 * equipped there is nothing to sacrifice, so the ability doesn't trigger.
 */
val ThirstingAxe = card("Thirsting Axe") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +4/+0.\n" +
        "At the beginning of your end step, if equipped creature didn't deal combat damage to a creature this turn, sacrifice it.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(4, 0, GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.All(
            Conditions.EntityMatches(EffectTarget.EquippedCreature, GameObjectFilter.Creature),
            Conditions.Not(
                Conditions.EntityMatches(
                    EffectTarget.EquippedCreature,
                    GameObjectFilter.Creature.hasDealtDamageThisTurn(
                        combatOnly = true,
                        to = DamageRecipientKind.Creature
                    )
                )
            )
        )
        effect = Effects.SacrificeTarget(EffectTarget.EquippedCreature)
        description = "At the beginning of your end step, if equipped creature didn't deal combat damage " +
            "to a creature this turn, sacrifice it."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Bastien L. Deharme"
        flavorText = "Sometimes the weapon wields the bearer."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f47e5303-c07a-4be0-a314-bf5739a856e5.jpg?1783937424"
    }
}
