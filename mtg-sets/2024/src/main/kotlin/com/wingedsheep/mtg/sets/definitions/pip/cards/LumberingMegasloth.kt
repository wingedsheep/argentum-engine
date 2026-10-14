package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lumbering Megasloth
 * {10}{G}{G}
 * Creature — Sloth Mutant
 * 8/8
 * This spell costs {1} less to cast for each counter among players and permanents.
 * Trample
 * This creature enters tapped.
 *
 * "Each counter among players and permanents" counts every kind on every player (a `null`
 * counter type on [DynamicAmounts.playerCounterCount]) plus every counter on every permanent.
 * Only the generic part shrinks; {G}{G} is always paid.
 */
val LumberingMegasloth = card("Lumbering Megasloth") {
    manaCost = "{10}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Sloth Mutant"
    power = 8
    toughness = 8
    oracleText = "This spell costs {1} less to cast for each counter among players and permanents.\n" +
        "Trample\n" +
        "This creature enters tapped."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(
                    DynamicAmounts.playerCounterCount(null, Player.Each) +
                        DynamicAmounts.battlefield(Player.Each).totalCounters()
                ),
            ),
        )
    }

    keywords(Keyword.TRAMPLE)

    replacementEffect(EntersTapped())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Daniel Romanovsky"
        flavorText = "When threatened, it slowly shakes its body to produce spores from the mushrooms growing on its back."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53b92e8a-fadf-4568-8b30-2df68e6ff7cf.jpg?1783912397"
    }
}
