package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lilypad Village
 * Land
 * {T}: Add {C}.
 * {T}: Add {U}. Spend this mana only to cast a creature spell.
 * {U}, {T}: Surveil 2. Activate only if a Bird, Frog, Otter, or Rat entered the
 * battlefield under your control this turn.
 */
val LilypadVillage = card("Lilypad Village") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}: Add {U}. Spend this mana only to cast a creature spell.\n{U}, {T}: Surveil 2. Activate only if a Bird, Frog, Otter, or Rat entered the battlefield under your control this turn."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLUE, restriction = ManaRestriction.CreatureSpellsOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        effect = EffectPatterns.surveil(2)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Exists(
                    player = Player.You,
                    zone = Zone.BATTLEFIELD,
                    filter = GameObjectFilter.Creature
                        .withAnyOfSubtypes(
                            listOf(
                                Subtype("Bird"),
                                Subtype("Frog"),
                                Subtype("Otter"),
                                Subtype("Rat")
                            )
                        )
                        .enteredThisTurn()
                        .youControl()
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "Alexander Forssberg"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e95a7cc-ed77-4ca4-80db-61c0fc68bf50.jpg?1721639529"
    }
}
