package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Pestbrood Sloth
 * {3}{G}
 * Creature — Plant Sloth
 * 4/4
 *
 * Reach
 * When this creature dies, create two 1/1 black and green Pest creature tokens with
 * "Whenever this token attacks, you gain 1 life."
 *
 * The dies trigger creates two Pest tokens, each carrying its own self-attack life-gain trigger via
 * [CreateTokenEffect.triggeredAbilities] (`Triggers.Attacks` SELF binding → `Effects.GainLife(1)`).
 */
val PestbroodSloth = card("Pestbrood Sloth") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Sloth"
    power = 4
    toughness = 4
    oracleText = "Reach\n" +
        "When this creature dies, create two 1/1 black and green Pest creature tokens with " +
        "\"Whenever this token attacks, you gain 1 life.\""

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(2),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Pest"),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.Attacks.event,
                    binding = Triggers.Attacks.binding,
                    effect = Effects.GainLife(1)
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/a/ba854032-6ad2-4654-990a-64006e7f92fd.jpg?1777982237"
        )
        description = "When this creature dies, create two 1/1 black and green Pest creature " +
            "tokens with \"Whenever this token attacks, you gain 1 life.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Alexandre Honoré"
        flavorText = "The sloths of Titan's Grave move so infrequently that entire ecosystems " +
            "develop in their fur."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1251ae6-2f19-4f84-ab02-6a6cc7ce6056.jpg?1775938073"
    }
}
