package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Bone Picker
 * {3}{B}
 * Creature — Bird
 * 3/2
 * This spell costs {3} less to cast if a creature died this turn.
 * Flying, deathtouch
 *
 * A self-cast [ModifySpellCost] reducing {3} generic, gated by [CostGating.OnlyIf] on the
 * table-wide [Conditions.CreatureDiedThisTurn] — "a creature", so an opponent's creature dying
 * turns the discount on too.
 */
val BonePicker = card("Bone Picker") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Bird"
    oracleText = "This spell costs {3} less to cast if a creature died this turn.\nFlying, deathtouch"
    power = 3
    toughness = 2

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(3),
            gating = CostGating.OnlyIf(Conditions.CreatureDiedThisTurn)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Yeong-Hao Han"
        flavorText = "They are the first to greet dissenters on their journey into exile."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdc6a825-43f7-40a4-95f0-335dc538b6cd.jpg"
    }
}
