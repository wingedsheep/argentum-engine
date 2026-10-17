package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Three Dog, Galaxy News DJ — the {2} and the Aura sacrifice are one reflexive action; "when you
 * sacrifice an Aura this way" is a separate reflexive trigger (CR 603.12) whose
 * [EffectTarget.SacrificedAsCost] still names the Aura after it is gone, copied from the copiable
 * values frozen as it was sacrificed — a sacrificed Aura token included. Each token enters attached
 * to its creature without targeting (shroud doesn't stop it); one that couldn't legally enchant
 * that creature isn't created (CR 303.4i).
 */
val ThreeDogGalaxyNewsDj = card("Three Dog, Galaxy News DJ") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Bard"
    power = 1
    toughness = 5
    oracleText = "Whenever you attack, you may pay {2} and sacrifice an Aura attached to Three Dog. " +
        "When you sacrifice an Aura this way, for each other attacking creature you control, create a " +
        "token that's a copy of that Aura attached to that creature."

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{2}") then Effects.SacrificeOwn(
                GameObjectFilter.Enchantment.withSubtype("Aura").attachedToSource()
            ),
            reflexiveEffect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl().attacking(), excludeSelf = true),
                Effects.CreateTokenCopyOfTarget(
                    EffectTarget.SacrificedAsCost(0),
                    attachedTo = EffectTarget.IterationEntity
                )
            ),
            optional = true,
            descriptionOverride = "For each other attacking creature you control, create a token that's " +
                "a copy of the sacrificed Aura attached to that creature."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Elizabeth Peiró"
        flavorText = "\"You're listening to Galaxy News Radio, bringing you the truth, no matter how bad it hurts.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab34e869-be7a-4632-95b3-5f200b4edcca.jpg?1783912383"
        ruling(
            "2024-03-08",
            "Since Three Dog's reflexive triggered ability doesn't target, Aura tokens created by that " +
                "ability will be attached to attacking creatures you control with shroud."
        )
        ruling(
            "2024-03-08",
            "If one of the Aura tokens being created couldn't legally be attached to the appropriate " +
                "attacking creature (because of an ability like protection from enchantments), that Aura " +
                "token won't be created."
        )
        ruling(
            "2024-03-08",
            "The tokens copy exactly what was printed on the original Aura and nothing else (unless that " +
                "Aura was itself copying something else; see below). It doesn't copy whether that Aura was " +
                "tapped or untapped, whether it had any counters on it or Auras attached to it, or any " +
                "non-copy effects that changed its types, color, and so on."
        )
        ruling(
            "2024-03-08",
            "If the copied Aura was copying something else, the tokens enter the battlefield as whatever " +
                "that Aura was copying."
        )
        ruling("2024-03-08", "If the copied Aura had {X} in its mana cost, X is 0.")
        ruling(
            "2024-03-08",
            "Any enters-the-battlefield abilities of the copied Aura will trigger when the tokens enter the " +
                "battlefield. Any \"as [this Aura] enters the battlefield\" or \"[this Aura] enters the " +
                "battlefield with\" abilities of the Aura will also work."
        )
    }
}
