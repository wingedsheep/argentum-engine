package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Obscuring Aether
 * {G}
 * Enchantment
 * Face-down creature spells you cast cost {1} less to cast.
 * {1}{G}: Turn this enchantment face down. (It becomes a 2/2 creature.)
 *
 * Face down it is a 2/2 creature with no name, types or abilities, so it stops reducing costs.
 * It has no morph cost, so nothing turns it back face up.
 */
val ObscuringAether = card("Obscuring Aether") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Face-down creature spells you cast cost {1} less to cast.\n" +
        "{1}{G}: Turn this enchantment face down. (It becomes a 2/2 creature.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.FaceDownYouCast,
            modification = CostModification.ReduceGeneric(1),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.TurnFaceDown(target = EffectTarget.Self)
        description = "{1}{G}: Turn this enchantment face down."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bff71e3-b309-4659-9d43-7a0d8ada1efc.jpg?1783938578"
        ruling("2015-02-25", "Obscuring Aether has no effect on spells that instruct you to manifest cards.")
        ruling("2015-02-25", "Note that Obscuring Aether doesn't have a morph cost. Once it's turned face down, it doesn't have a way to turn itself face up.")
    }
}
