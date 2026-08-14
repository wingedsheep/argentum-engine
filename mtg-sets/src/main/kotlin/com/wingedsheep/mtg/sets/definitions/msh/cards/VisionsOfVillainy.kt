package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Visions of Villainy — Marvel Super Heroes #120
 * {2}{B} · Instant · Common
 *
 * This spell costs {1} less to cast if you control a Villain.
 * You draw two cards and lose 2 life.
 *
 * The discount is the Venom's Hunger shape: a [ModifySpellCost] static ability scoped to
 * [SpellCostTarget.SelfCast], reducing generic mana by
 * [CostReductionSource.FixedIfControlFilter] when a Villain permanent is on your battlefield.
 * It's checked as the spell is cast (CR 601.2f), so a Villain that leaves in response doesn't
 * refund or surcharge.
 *
 * "You draw two cards and lose 2 life" is a single non-optional composite — one player, both halves,
 * no targets — so it never fizzles and the life loss happens even if the draw empties the library.
 */
val VisionsOfVillainy = card("Visions of Villainy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if you control a Villain.\n" +
        "You draw two cards and lose 2 life."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfControlFilter(
                    amount = 1,
                    filter = GameObjectFilter.Permanent.withSubtype(Subtype.VILLAIN)
                )
            )
        )
    }

    spell {
        effect = Effects.Composite(
            Effects.DrawCards(2, EffectTarget.Controller),
            Effects.LoseLife(2, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Pavel Kolomeyets"
        flavorText = "As Thanos gazed into the cosmic vortex of the Infinity Well, he beheld the " +
            "means to serve Death all the better."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8f1e4f9-7415-437f-b694-ecbdd76db114.jpg?1783902935"
    }
}
