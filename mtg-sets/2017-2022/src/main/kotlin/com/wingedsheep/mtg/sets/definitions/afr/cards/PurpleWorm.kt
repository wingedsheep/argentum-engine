package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Purple Worm
 * {5}{G}{G}
 * Creature — Worm
 * 8/7
 * This spell costs {2} less to cast if a creature died this turn.
 * Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)
 */
val PurpleWorm = card("Purple Worm") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Worm"
    oracleText = "This spell costs {2} less to cast if a creature died this turn.\n" +
        "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)"
    power = 8
    toughness = 7
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.CreatureDiedThisTurn),
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Olivier Bernard"
        flavorText = "Underground civilizations maintain magical barriers around their settlements to deter these monsters."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e419d58b-6241-4892-9080-f6894ac7ba89.jpg?1783926456"
    }
}
