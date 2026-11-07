package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Metallurgic Summonings
 * {3}{U}{U}
 * Enchantment
 * Whenever you cast an instant or sorcery spell, create an X/X colorless Construct artifact
 * creature token, where X is that spell's mana value.
 * {3}{U}{U}, Exile this enchantment: Return all instant and sorcery cards from your graveyard to
 * your hand. Activate only if you control six or more artifacts.
 *
 * X reads the triggering spell's mana value as cast (its {X} counted at the chosen value), captured
 * with the trigger, so it still holds if the spell is countered before the trigger resolves — the
 * same shape as Shark Typhoon. The artifact count is an activation restriction, checked only as the
 * ability is activated (per the rulings), not again on resolution.
 */
val MetallurgicSummonings = card("Metallurgic Summonings") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an instant or sorcery spell, create an X/X colorless Construct " +
        "artifact creature token, where X is that spell's mana value.\n" +
        "{3}{U}{U}, Exile this enchantment: Return all instant and sorcery cards from your graveyard " +
        "to your hand. Activate only if you control six or more artifacts."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            creatureTypes = setOf("Construct"),
            artifactToken = true,
            dynamicPower = DynamicAmounts.triggeringSpellManaValue(),
            dynamicToughness = DynamicAmounts.triggeringSpellManaValue(),
            imageUri = "https://cards.scryfall.io/normal/front/6/2/623a08d1-f5ff-48b7-bdb6-54b8d7a4b931.jpg?1783937240",
        )
        description = "Whenever you cast an instant or sorcery spell, create an X/X colorless Construct " +
            "artifact creature token, where X is that spell's mana value."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{U}{U}"), Costs.ExileSelf)
        effect = Effects.Pipeline {
            val cards = gather(
                CardSource.FromZone(Zone.GRAVEYARD, filter = GameObjectFilter.InstantOrSorcery)
            )
            toHand(cards)
        }
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.YouControlAtLeast(6, GameObjectFilter.Artifact))
        )
        description = "Return all instant and sorcery cards from your graveyard to your hand. " +
            "Activate only if you control six or more artifacts."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "56"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0ac06f9-5c9f-4d4f-be57-7c58bd8da568.jpg?1783937216"
        ruling(
            "2016-09-20",
            "For a spell with {X} in its mana cost, use the value chosen for X to determine its mana value. " +
                "For example, Clash of Wills is an instant spell with mana cost {X}{U}. If you chose 2 as the " +
                "value of X, then Clash of Wills has mana value 3, and Metallurgic Summonings's ability will " +
                "create a 3/3 token."
        )
        ruling(
            "2016-09-20",
            "Metallurgic Summonings's first ability resolves before the spell that caused it to trigger."
        )
        ruling(
            "2016-09-20",
            "If Metallurgic Summonings's first ability resolves and the spell that caused it to trigger has " +
                "been countered, use that spell's mana value as it last existed on the stack to determine the " +
                "value of X."
        )
        ruling(
            "2016-09-20",
            "The number of artifacts you control is checked only as you activate Metallurgic Summonings's " +
                "last ability. It's not checked again as it resolves."
        )
    }
}
