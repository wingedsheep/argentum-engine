package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Frodo, Determined Hero
 * {1}{W}
 * Legendary Creature — Halfling Warrior
 * 2/2
 *
 * Whenever Frodo enters or attacks, you may attach target Equipment you control with mana value
 * 2 or 3 to Frodo.
 * During your turn, prevent all damage that would be dealt to Frodo.
 *
 * Composed from existing primitives: the "enters or attacks" ability is modeled as two sibling
 * triggered abilities (the engine has no combined enters-or-attacks trigger), each attaching an
 * optional ("you may … target") Equipment-you-control with mana value 2 or 3 to Frodo (self). The
 * damage shield is a [PreventDamage] replacement scoped to Frodo ([RecipientFilter.Self]) and gated
 * to your turn ([IsYourTurn] restriction).
 */
val FrodoDeterminedHero = card("Frodo, Determined Hero") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Halfling Warrior"
    power = 2
    toughness = 2
    oracleText = "Whenever Frodo enters or attacks, you may attach target Equipment you control with mana value 2 or 3 to Frodo.\n" +
        "During your turn, prevent all damage that would be dealt to Frodo."

    // "Whenever Frodo enters …"
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val equipment = target(
            "Equipment you control with mana value 2 or 3",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()
                        .manaValueAtLeast(2).manaValueAtMost(3)
                ),
                optional = true
            )
        )
        effect = Effects.AttachTargetEquipmentToCreature(equipment, EffectTarget.Self)
    }

    // "… or attacks"
    triggeredAbility {
        trigger = Triggers.Attacks
        val equipment = target(
            "Equipment you control with mana value 2 or 3",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()
                        .manaValueAtLeast(2).manaValueAtMost(3)
                ),
                optional = true
            )
        )
        effect = Effects.AttachTargetEquipmentToCreature(equipment, EffectTarget.Self)
    }

    // "During your turn, prevent all damage that would be dealt to Frodo."
    replacementEffect(
        PreventDamage(
            restrictions = listOf(IsYourTurn),
            appliesTo = EventPattern.DamageEvent(recipient = RecipientFilter.Self)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "289"
        flavorText = "Frodo's heart flamed within him. He drew his sword and advanced."
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83d1b37c-a8c6-4690-8701-3395397711e7.jpg?1687424782"
    }
}
