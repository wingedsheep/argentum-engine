package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Tracker
 * {2}{G}
 * Creature — Human
 * 2/2
 * {G}{G}, {T}: This creature deals damage equal to its power to target creature. That creature
 * deals damage equal to its power to this creature.
 *
 * This is a fight in all but name, and deliberately **not** `Effects.Fight`. CR 701.14b makes a
 * fight all-or-nothing: if either creature has left the battlefield when the ability resolves,
 * neither deals damage. Tracker's printed text is two ordinary sentences, so each half stands on
 * its own — if the Tracker is gone the target still takes nothing from it, but the target's own
 * damage back is a separate instruction. Modelling it as two `DealDamage` steps keeps that.
 *
 * Both amounts read power at resolution off their respective dealer, and `damageSource` names the
 * dealer so prevention, protection and "damage dealt by" triggers see the right source.
 *
 * No SBA pass runs between sub-effects, so a target killed by the first half is still on the
 * battlefield to deal its damage back — which is how the card has always worked.
 */
val Tracker = card("Tracker") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human"
    power = 2
    toughness = 2
    oracleText = "{G}{G}, {T}: This creature deals damage equal to its power to target creature. " +
        "That creature deals damage equal to its power to this creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{G}"), Costs.Tap)
        val prey = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.DealDamage(
                DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power),
                prey,
                damageSource = EffectTarget.Self,
            ),
            Effects.DealDamage(
                DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power),
                EffectTarget.Self,
                damageSource = prey,
            ),
        )
        description = "{G}{G}, {T}: This creature deals damage equal to its power to target " +
            "creature. That creature deals damage equal to its power to this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35ffc69e-26f2-434f-8c89-2df108dd984a.jpg?1783947929"
    }
}
