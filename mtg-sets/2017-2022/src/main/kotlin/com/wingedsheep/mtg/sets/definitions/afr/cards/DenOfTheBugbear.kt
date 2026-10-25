package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Den of the Bugbear
 * Land
 * If you control two or more other lands, this land enters tapped.
 * {T}: Add {R}.
 * {3}{R}: Until end of turn, this land becomes a 3/2 red Goblin creature with "Whenever this
 * creature attacks, create a 1/1 red Goblin creature token that's tapped and attacking."
 * It's still a land.
 *
 * Same shape as Hive of the Eye Tyrant: [Effects.BecomeCreature] then
 * [Effects.GrantTriggeredAbility] until end of turn. The token rides the Hanweir Garrison
 * `tapped = true, attacking = true` path.
 */
val DenOfTheBugbear = card("Den of the Bugbear") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "If you control two or more other lands, this land enters tapped.\n{T}: Add {R}.\n" +
        "{3}{R}: Until end of turn, this land becomes a 3/2 red Goblin creature with \"Whenever this creature " +
        "attacks, create a 1/1 red Goblin creature token that's tapped and attacking.\" It's still a land."

    replacementEffect(
        EntersTapped(unlessCondition = Conditions.Not(Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 2,
            creatureTypes = setOf("Goblin"),
            colors = setOf("RED"),
            duration = Duration.EndOfTurn,
        ) then Effects.GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.attacks(),
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Goblin"),
                    tapped = true,
                    attacking = true,
                ),
                descriptionOverride = "Whenever this creature attacks, create a 1/1 red Goblin creature token " +
                    "that's tapped and attacking.",
            ),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
        description = "{3}{R}: Until end of turn, this land becomes a 3/2 red Goblin creature with \"Whenever this " +
            "creature attacks, create a 1/1 red Goblin creature token that's tapped and attacking.\" It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "254"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f231caf8-56c0-4719-a90d-5e5efbee3148.jpg?1783926435"
    }
}
