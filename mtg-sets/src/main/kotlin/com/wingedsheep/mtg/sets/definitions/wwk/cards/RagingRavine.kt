package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.EventPattern.AttackEvent
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Raging Ravine
 * Land
 *
 * This land enters tapped.
 * {T}: Add {R} or {G}.
 * {2}{R}{G}: Until end of turn, this land becomes a 3/3 red and green Elemental creature
 * with "Whenever this creature attacks, put a +1/+1 counter on it." It's still a land.
 */
val RagingRavine = card("Raging Ravine") {
    typeLine = "Land"
    colorIdentity = "RG"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {R} or {G}.\n" +
        "{2}{R}{G}: Until end of turn, this land becomes a 3/3 red and green Elemental creature " +
        "with \"Whenever this creature attacks, put a +1/+1 counter on it.\" It's still a land."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}{G}")
        effect = Effects.Composite(
            listOf(
                BecomeCreatureEffect(
                    target = EffectTarget.Self,
                    power = 3,
                    toughness = 3,
                    creatureTypes = setOf("Elemental"),
                    colors = setOf(Color.RED.name, Color.GREEN.name),
                    duration = Duration.EndOfTurn,
                ),
                GrantTriggeredAbilityEffect(
                    ability = TriggeredAbility.create(
                        trigger = AttackEvent(),
                        binding = TriggerBinding.SELF,
                        effect = AddCountersEffect(
                            counterType = Counters.PLUS_ONE_PLUS_ONE,
                            count = 1,
                            target = EffectTarget.Self,
                        ),
                        descriptionOverride = "Whenever this creature attacks, put a +1/+1 counter on it.",
                    ),
                    target = EffectTarget.Self,
                    duration = Duration.EndOfTurn,
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb7b6fb5-4d38-4734-b46f-923322cff24e.jpg?1764758780"

        ruling("2010-03-01", "If Raging Ravine becomes a creature but you haven't controlled it continuously since your most recent turn began, you won't be able to activate its mana ability or attack with it that turn.")
        ruling("2010-03-01", "Any +1/+1 counters put on Raging Ravine remain on it even after it stops being a creature. They'll have no effect until it becomes a creature again.")
        ruling("2010-03-01", "If Raging Ravine's last ability is activated multiple times in a turn, it will get that many instances of the granted triggered ability.")
    }
}
