package com.wingedsheep.mtg.sets.definitions.cn2.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Selvala, Heart of the Wilds — Conspiracy: Take the Crown #70 (canonical / earliest real printing, 2016)
 * {1}{G}{G} · Legendary Creature — Elf Scout · 2/3
 *
 * Whenever another creature enters, its controller may draw a card if its power is greater than
 * each other creature's power.
 * {G}, {T}: Add X mana in any combination of colors, where X is the greatest power among creatures
 * you control.
 *
 * **Draw trigger** — `Triggers.another(Creature).enters()` binds the entering creature (any
 * controller) as the triggering entity. The "if" is not an intervening-if: it is checked once, on
 * resolution (ruling), as "no other creature on the battlefield has power ≥ its power" — a count of
 * zero rather than a `maxPower()` comparison, so it stays vacuously true when there is no other
 * creature at all. Its power is read through `EntityProperty(TriggeringEntity, Power)`, which falls
 * back to last-known information if it has left the battlefield (ruling). The yes/no goes to
 * [EffectTarget.ControllerOfTriggeringEntity], who is also the one who draws.
 *
 * **Mana ability** — `AddManaInAnyCombination` over the greatest projected power among your
 * creatures; a total of 0 or less adds nothing (ruling).
 */
val SelvalaHeartOfTheWilds = card("Selvala, Heart of the Wilds") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Scout"
    power = 2
    toughness = 3
    oracleText = "Whenever another creature enters, its controller may draw a card if its power is " +
        "greater than each other creature's power.\n" +
        "{G}, {T}: Add X mana in any combination of colors, where X is the greatest power among " +
        "creatures you control."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).enters()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.battlefield(
                    Player.Each,
                    GameObjectFilter.Creature
                        .notTriggeringEntity()
                        .compareNumericProperty(
                            CardNumericProperty.POWER,
                            ComparisonOperator.GTE,
                            DynamicAmounts.propertyOf(EffectTarget.TriggeringEntity, EntityNumericProperty.Power),
                        ),
                ).count(),
                ComparisonOperator.EQ,
                0,
            ),
            then = Effects.May(
                Effects.DrawCards(1, EffectTarget.ControllerOfTriggeringEntity),
                decisionMaker = EffectTarget.ControllerOfTriggeringEntity,
            ),
        )
        description = "Whenever another creature enters, its controller may draw a card if its power " +
            "is greater than each other creature's power."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        effect = Effects.AddManaInAnyCombination(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Add X mana in any combination of colors, where X is the greatest power among " +
            "creatures you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "70"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99a3e619-fbdb-406c-9f21-eb582cb878c0.jpg?1783937342"
        ruling(
            "2016-08-23",
            "Selvala's last ability is a mana ability. It doesn't use the stack and can't be responded to. " +
                "If the greatest power among creatures you control is 0 or less at that time, no mana is added."
        )
        ruling(
            "2016-08-23",
            "The new creature's power is compared to the power of each other creature on the battlefield as " +
                "the first ability resolves. If another creature has the same or higher power than the new " +
                "creature's power, no one may draw a card."
        )
        ruling(
            "2016-08-23",
            "If the new creature isn't on the battlefield as the first ability resolves, use its power when " +
                "it left the battlefield to determine whether its controller may draw a card. Note that effects " +
                "that reduced its power before it left the battlefield will apply."
        )
    }
}
