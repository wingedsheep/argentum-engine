package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Warren Warleader
 * {2}{W}{W}
 * Creature — Rabbit Knight
 * 4/4
 *
 * Offspring {2} (You may pay an additional {2} as you cast this spell. If you do,
 * when this creature enters, create a 1/1 token copy of it.)
 *
 * Whenever you attack, choose one —
 * • Create a 1/1 white Rabbit creature token that's tapped and attacking.
 * • Attacking creatures you control get +1/+1 until end of turn.
 */
val WarrenWarleader = card("Warren Warleader") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rabbit Knight"
    power = 4
    toughness = 4
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nWhenever you attack, choose one —\n• Create a 1/1 white Rabbit creature token that's tapped and attacking.\n• Attacking creatures you control get +1/+1 until end of turn."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.OptionalAdditionalCost(ManaCost.parse("{2}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // Whenever you attack, choose one
    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = ModalEffect.chooseOne(
            // Create a 1/1 white Rabbit creature token that's tapped and attacking
            Mode.noTarget(
                CreateTokenEffect(
                    count = DynamicAmount.Fixed(1),
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Rabbit"),
                    tapped = true,
                    attacking = true,
                    imageUri = "https://cards.scryfall.io/normal/front/8/1/81de52ef-7515-4958-abea-fb8ebdcef93c.jpg?1721431122"
                ),
                "Create a 1/1 white Rabbit creature token that's tapped and attacking"
            ),
            // Attacking creatures you control get +1/+1 until end of turn
            Mode.noTarget(
                Effects.ForEachInGroup(
                    filter = GroupFilter.AllCreaturesYouControl.attacking(),
                    effect = ModifyStatsEffect(
                        powerModifier = 1,
                        toughnessModifier = 1,
                        target = EffectTarget.Self,
                        duration = Duration.EndOfTurn
                    )
                ),
                "Attacking creatures you control get +1/+1 until end of turn"
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "38"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb5237a0-5ac3-4ded-9f92-5f782a7bbbd7.jpg?1721425996"
        ruling("2024-07-26", "You choose the player, planeswalker, or battle the Rabbit token is attacking. It doesn't have to be the same player, planeswalker, or battle that any other attacking creature is attacking.")
        ruling("2024-07-26", "Although the token enters attacking, it was never declared as an attacking creature. Abilities that trigger whenever a creature attacks won't trigger when that creature enters attacking.")
    }
}
