package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Hell to Pay {X}{R}
 * Sorcery
 *
 * Hell to Pay deals X damage to target creature. Create a number of tapped Treasure tokens
 * equal to the amount of excess damage dealt to that creature this way.
 *
 * Composed from existing atoms: [Effects.DealXDamage] deals X to the target and marks the
 * damage, then [Effects.CreateTreasure] reads the post-damage excess via
 * `EntityProperty(EntityReference.Target(0), ExcessMarkedDamage)` — `max(0, marked − toughness)`
 * (CR 120.4a). CompositeEffect resolves its steps sequentially with no interleaved SBA pass, so
 * the marked damage in scope at the second step is exactly the X this spell just dealt. No
 * bespoke "excess damage" executor — this mirrors Orbital Plunge's excess gate, but reads the
 * *amount* rather than a boolean.
 */
val HellToPay = card("Hell to Pay") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Hell to Pay deals X damage to target creature. Create a number of tapped " +
        "Treasure tokens equal to the amount of excess damage dealt to that creature this way."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.DealXDamage(creature),
            Effects.CreateTreasure(
                count = DynamicAmount.EntityProperty(
                    EntityReference.Target(0),
                    EntityNumericProperty.ExcessMarkedDamage
                ),
                tapped = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Liiga Smilshkalne"
        flavorText = "For years afterward, lucky explorers who found the cave could chip gold fragments off the rocky walls."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84ad8ed7-1429-432e-8217-a4db3b97675c.jpg?1712355764"

        ruling("2024-04-12", "Excess damage is the amount of damage dealt to the creature beyond what was needed to be lethal. Damage already marked on the creature and any damage that would be prevented are taken into account.")
        ruling("2024-04-12", "If the targeted creature is an illegal target by the time Hell to Pay tries to resolve, the spell doesn't resolve and you don't create any Treasure tokens.")
    }
}
