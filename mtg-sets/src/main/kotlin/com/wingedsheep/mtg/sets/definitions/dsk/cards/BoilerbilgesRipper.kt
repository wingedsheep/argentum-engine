package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Boilerbilges Ripper — Duskmourn: House of Horror #127
 * {4}{R}
 * Creature — Human Assassin
 * 4/4
 *
 * When this creature enters, you may sacrifice another creature or enchantment. When you do,
 * this creature deals 2 damage to any target.
 *
 * "Sacrifice another creature or enchantment" is a resolution-time choice, not a target: the
 * player accepts the optional first, then chooses which permanent (so declining never forces a
 * commitment). The "When you do" reflexive trigger then deals 2 damage to any target, chosen as
 * that second ability goes on the stack. The damage source is this creature.
 */
val BoilerbilgesRipper = card("Boilerbilges Ripper") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Assassin"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, you may sacrifice another creature or enchantment. " +
        "When you do, this creature deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = Effects.Composite(
                listOf(
                    SelectTargetEffect(
                        requirement = TargetObject(
                            filter = TargetFilter.CreatureOrEnchantment.youControl().other()
                        ),
                        storeAs = "permanentToSacrifice"
                    ),
                    Effects.SacrificeTarget(EffectTarget.PipelineTarget("permanentToSacrifice"))
                )
            ),
            optional = true,
            reflexiveEffect = Effects.DealDamage(
                amount = 2,
                target = EffectTarget.ContextTarget(0),
                damageSource = EffectTarget.Self
            ),
            reflexiveTargetRequirements = listOf(Targets.Any),
            descriptionOverride = "You may sacrifice another creature or enchantment. When you do, " +
                "this creature deals 2 damage to any target."
        )
        description = "When this creature enters, you may sacrifice another creature or enchantment. " +
            "When you do, this creature deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Kai Carpenter"
        flavorText = "\"Go ahead, meat. Run.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a68009c-83cd-455f-81e9-bdd720d23a43.jpg?1726286321"
    }
}
