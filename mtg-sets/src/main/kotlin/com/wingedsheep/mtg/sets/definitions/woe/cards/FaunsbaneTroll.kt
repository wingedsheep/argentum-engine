package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Faunsbane Troll
 * {2}{B}{G}
 * Creature — Troll
 * 4/4
 *
 * When this creature enters, create a Monster Role token attached to it.
 * {1}, Sacrifice an Aura attached to this creature: This creature fights target creature you
 * don't control. If that creature would die this turn, exile it instead. Activate only as a
 * sorcery.
 *
 * The enters trigger creates the Role directly on the Troll without targeting it. The activated
 * ability's sacrifice filter is source-relative: [GameObjectFilter.attachedToSource] accepts only
 * an Aura currently attached to this Troll, including the Monster Role it normally creates.
 *
 * [MarkExileOnDeathEffect] follows the fight in the ordered composite. If the Troll is gone or no
 * longer a creature when the ability resolves, the fight does nothing but the target is still
 * marked for exile, matching the card's ruling. The marker applies to any later death that turn,
 * not only one caused by the fight.
 */
val FaunsbaneTroll = card("Faunsbane Troll") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Troll"
    oracleText = "When this creature enters, create a Monster Role token attached to it. " +
        "(Enchanted creature gets +1/+1 and has trample.)\n" +
        "{1}, Sacrifice an Aura attached to this creature: This creature fights target creature " +
        "you don't control. If that creature would die this turn, exile it instead. Activate " +
        "only as a sorcery."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateRoleToken("Monster Role", EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Sacrifice(
                GameObjectFilter.Enchantment.withSubtype("Aura").attachedToSource()
            ),
        )
        timing = TimingRule.SorcerySpeed
        val creature = target("target creature you don't control", Targets.CreatureOpponentControls)
        effect = Effects.Composite(
            Effects.Fight(EffectTarget.Self, creature),
            MarkExileOnDeathEffect(creature),
        )
        description = "This creature fights target creature you don't control. If that creature " +
            "would die this turn, exile it instead. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d8bd585-c5ea-46f8-8e11-f33c067f2f8e.jpg?1783915072"

        ruling(
            "2023-09-01",
            "If Faunsbane Troll's activated ability resolves, its effect causes the creature to " +
                "be exiled any time it would die that turn, not just if it would die as a result " +
                "of damage dealt by Faunsbane Troll."
        )
        ruling(
            "2023-09-01",
            "If Faunsbane Troll is no longer on the battlefield or no longer a creature when its " +
                "activated ability resolves, it doesn't fight the target creature, but the effect " +
                "that causes the target creature to be exiled any time it would die that turn " +
                "still applies."
        )
    }
}
