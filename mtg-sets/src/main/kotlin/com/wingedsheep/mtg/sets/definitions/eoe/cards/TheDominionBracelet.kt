package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * The Dominion Bracelet
 * {2}
 * Legendary Artifact — Equipment
 * Equipped creature gets +1/+1 and has "{15}, Exile The Dominion Bracelet:
 * You control target opponent during their next turn. This ability costs {X}
 * less to activate, where X is this creature's power. Activate only as a
 * sorcery." (You see all cards that player could see and make all decisions
 * for them.)
 * Equip {1}
 */
val TheDominionBracelet = card("The Dominion Bracelet") {
    manaCost = "{2}"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 and has \"{15}, Exile The Dominion Bracelet: " +
        "You control target opponent during their next turn. This ability costs {X} less to " +
        "activate, where X is this creature's power. Activate only as a sorcery.\" " +
        "(You see all cards that player could see and make all decisions for them.)\n" +
        "Equip {1}"

    staticAbility {
        effect = Effects.ModifyStats(+1, +1)
        filter = Filters.EquippedCreature
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Composite(
                    listOf(
                        AbilityCost.Mana(ManaCost.parse("{15}")),
                        AbilityCost.ExileGrantingPermanent
                    )
                ),
                effect = Effects.HijackNextTurn(EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(TargetOpponent()),
                timing = TimingRule.SorcerySpeed,
                genericCostReduction = DynamicAmount.EntityProperty(
                    EntityReference.Source,
                    EntityNumericProperty.Power
                ),
                descriptionOverride = "{15}, Exile The Dominion Bracelet: You control target " +
                    "opponent during their next turn. This ability costs {X} less to activate, " +
                    "where X is this creature's power."
            )
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "239"
        artist = "Nathaniel Himawan"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5360880-2849-45d6-b1aa-08c7e01083af.jpg?1752947533"

        ruling("2025-07-25", "The cost of the activated ability granted by The Dominion Bracelet is locked in before costs are paid. For example, if the power of the equipped creature is 3 (including the +1/+1 granted by The Dominion Bracelet's ability), the granted ability will cost {12} to activate.")
        ruling("2025-07-25", "While controlling another player, you make all choices and decisions that player is allowed to make or is told to make during that turn. This includes choices about what spells to cast or what abilities to activate, as well as any decisions called for by triggered abilities or for any other reason.")
        ruling("2025-07-25", "You can't make the affected player concede. That player may choose to concede at any time, even while you're controlling that player.")
        ruling("2025-07-25", "You can use only the affected player's resources (cards, mana, and so on) to pay costs for that player; you can't use your own. Similarly, you can use the affected player's resources only to pay that player's costs; you can't spend them on your costs.")
        ruling("2025-07-25", "You only control the player. You don't control any of that player's permanents, spells, or abilities.")
        ruling("2025-07-25", "While controlling another player, you also continue to make your own choices and decisions.")
        ruling("2025-07-25", "The player you're controlling is still the active player during that turn.")
        ruling("2025-07-25", "If the targeted player skips their next turn, you'll control the next turn the affected player actually takes.")
        ruling("2025-07-25", "Multiple player-controlling effects that affect the same player overwrite each other. The last one to be created is the one that works.")
    }
}
