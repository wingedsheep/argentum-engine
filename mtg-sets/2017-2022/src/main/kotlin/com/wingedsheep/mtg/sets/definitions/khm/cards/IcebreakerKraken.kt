package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Icebreaker Kraken
 * {10}{U}{U}
 * Snow Creature — Kraken
 * 8/8
 * Affinity for snow lands (This spell costs {1} less to cast for each snow land you control.)
 * When this creature enters, artifacts and creatures target opponent controls don't untap during
 * that player's next untap step.
 * Return three snow lands you control to their owner's hand: Return this creature to its owner's hand.
 *
 * The freeze is matched at that player's untap step, so it also holds artifacts and creatures they
 * gain control of after it resolved (2021-02-05 ruling). It taps nothing.
 */
val IcebreakerKraken = card("Icebreaker Kraken") {
    manaCost = "{10}{U}{U}"
    colorIdentity = "U"
    typeLine = "Snow Creature — Kraken"
    oracleText = "Affinity for snow lands (This spell costs {1} less to cast for each snow land you control.)\n" +
        "When this creature enters, artifacts and creatures target opponent controls don't untap during that player's next untap step.\n" +
        "Return three snow lands you control to their owner's hand: Return this creature to its owner's hand."
    power = 8
    toughness = 8

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsYouControlMatching(GameObjectFilter.Land.snow())
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.SkipUntap(opponent, GameObjectFilter.Artifact or GameObjectFilter.Creature)
    }

    activatedAbility {
        cost = Costs.ReturnToHand(GameObjectFilter.Land.snow(), count = 3)
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1fdb9bb-09a2-4ff7-bcd4-35ea33c1b752.jpg?1783928261"
    }
}
