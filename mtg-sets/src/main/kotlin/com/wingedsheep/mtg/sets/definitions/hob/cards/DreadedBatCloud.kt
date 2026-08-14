package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Dreaded Bat-Cloud
 * {4}{B}
 * Creature — Bat
 * 4/2
 * This spell costs {3} less to cast if a creature died this turn.
 * Flying, deathtouch
 *
 * The cost reduction is a [SpellCostTarget.SelfCast] static, live from any zone the spell is cast
 * from, and reads the table-wide died-this-turn tally via
 * [CostReductionSource.FixedIfCreatureDiedThisTurn] — the oracle text says "a creature", not "a
 * creature you controlled", so an opponent's creature dying turns this on too.
 */
val DreadedBatCloud = card("Dreaded Bat-Cloud") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat"
    oracleText = "This spell costs {3} less to cast if a creature died this turn.\nFlying, deathtouch"
    power = 4
    toughness = 2

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfCreatureDiedThisTurn(3)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Andreia Ugrai"
        flavorText = "Beneath the thunder another blackness whirled forward, so dense that no light could be seen between their wings."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67d52db5-597e-46d5-af39-c3a2de107d30.jpg?1785497085"
    }
}
