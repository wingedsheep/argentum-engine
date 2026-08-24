package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Desperate Charge
 * {2}{B}
 * Sorcery
 *
 * The mass-pump shape: one [Effects.ForEachInGroup] over the creatures you control, whose
 * per-member body is a [Effects.ModifyStats] on `EffectTarget.Self` — the iterated member, not the
 * spell's controller. The +2/+0 lasts until end of turn, which is `ModifyStats`' default duration.
 */
val DesperateCharge = card("Desperate Charge") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Creatures you control get +2/+0 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(2, 0, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Chen Weidong"
        flavorText = "\"Lieutenants dishonored, corpses carted home; The general raises troops again to take revenge.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5587089-b17e-4187-97aa-1c3eec070a0b.jpg"
    }
}
