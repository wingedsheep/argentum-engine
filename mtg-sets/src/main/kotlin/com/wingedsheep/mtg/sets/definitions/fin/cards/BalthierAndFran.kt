package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Balthier and Fran
 * {1}{R}{G}
 * Legendary Creature — Human Rabbit
 * 4/3
 * Reach
 * Vehicles you control get +1/+1 and have vigilance and reach.
 * Whenever a Vehicle crewed by Balthier and Fran this turn attacks, if it's the first combat phase
 *   of the turn, you may pay {1}{R}{G}. If you do, after this phase, there is an additional combat
 *   phase.
 *
 * The static lord is three battlefield-scoped static abilities over `Vehicles you control`
 * ([GameObjectFilter.Any] `.withAnySubtype("Vehicle")` — an uncrewed Vehicle is a noncreature
 * artifact but still gets the buff, exactly like Miriam, Herd Whisperer's hexproof grant): a
 * [ModifyStats] +1/+1 plus two [GrantKeyword]s for vigilance and reach.
 *
 * The attack trigger reuses the Miriam per-attacker idiom: `Triggers.attacks(filter, binding = ANY)`
 * fires once per attacking Vehicle, evaluating the filter against each attacker with this card as
 * the predicate source. The filter's new [GameObjectFilter.crewedOrSaddledBySourceThisTurn] gate is
 * the source-relative mirror of `crewedOrSaddledSourceThisTurn` — it matches only Vehicles whose
 * `CrewSaddleContributorsComponent` (recorded by the crew handler) contains this card, i.e. Vehicles
 * that Balthier and Fran themselves crewed this turn. The intervening "if it's the first combat phase
 * of the turn" is the new [Conditions.IsFirstCombatPhaseOfTurn] loop guard (true only in the natural
 * combat phase, false in the extra phase this rider spawns). The optional {1}{R}{G} + "after this
 * phase, there is an additional combat phase" is `MayPayManaEffect` gating [Effects.AddCombatPhase].
 */
val BalthierAndFran = card("Balthier and Fran") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Rabbit"
    power = 4
    toughness = 3
    oracleText = "Reach\n" +
        "Vehicles you control get +1/+1 and have vigilance and reach.\n" +
        "Whenever a Vehicle crewed by Balthier and Fran this turn attacks, if it's the first combat " +
        "phase of the turn, you may pay {1}{R}{G}. If you do, after this phase, there is an " +
        "additional combat phase."

    keywords(Keyword.REACH)

    // "Vehicles you control get +1/+1 and have vigilance and reach."
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Any.withAnySubtype("Vehicle").youControl())
        )
    }
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter(GameObjectFilter.Any.withAnySubtype("Vehicle").youControl())
        )
    }
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.REACH,
            filter = GroupFilter(GameObjectFilter.Any.withAnySubtype("Vehicle").youControl())
        )
    }

    // Whenever a Vehicle crewed by Balthier and Fran this turn attacks, if it's the first combat
    // phase of the turn, you may pay {1}{R}{G}. If you do, after this phase, there is an additional
    // combat phase.
    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Any.withAnySubtype("Vehicle").youControl()
                .crewedOrSaddledBySourceThisTurn(),
            binding = TriggerBinding.ANY
        )
        triggerCondition = Conditions.IsFirstCombatPhaseOfTurn
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}{R}{G}"),
            effect = Effects.AddCombatPhase
        )
        description = "Whenever a Vehicle crewed by Balthier and Fran this turn attacks, if it's " +
            "the first combat phase of the turn, you may pay {1}{R}{G}. If you do, after this " +
            "phase, there is an additional combat phase."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Arif Wijaya"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afcaed7d-7ea3-4f2a-a7f5-ee3315226369.jpg?1782686436"
    }
}
