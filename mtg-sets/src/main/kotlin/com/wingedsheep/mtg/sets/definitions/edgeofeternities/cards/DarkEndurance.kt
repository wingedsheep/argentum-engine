package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.SpellCostReduction
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dark Endurance
 * {1}{B}
 * Instant
 * This spell costs {1} less to cast if it targets a blocking creature.
 * Target creature gets +2/+0 and gains indestructible until end of turn. (Damage and effects that say "destroy" don't destroy it.)
 */
val DarkEndurance = card("Dark Endurance") {
    manaCost = "{1}{B}"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if it targets a blocking creature.\nTarget creature gets +2/+0 and gains indestructible until end of turn. (Damage and effects that say \"destroy\" don't destroy it.)"

    // Cost reduction for blocking creatures
    staticAbility {
        ability = SpellCostReduction(
            CostReductionSource.FixedIfAnyTargetMatches(
                amount = 1,
                filter = GameObjectFilter.Creature.blocking()
            )
        )
    }

    // Main spell effect
    spell {
        val target = target("target creature", Targets.Creature)
        
        effect = Effects.ModifyStats(2, 0, target)
            .then(Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, target))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Leon Tukker"
        flavorText = "One last chance. Make it count."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fac87b49-a0cd-42d5-b30a-efc6d5526fc3.jpg?1753360523"
    }
}
