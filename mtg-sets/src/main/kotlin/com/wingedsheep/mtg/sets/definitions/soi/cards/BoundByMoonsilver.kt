package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bound by Moonsilver
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack, block, or transform.
 * Sacrifice another permanent: Attach this Aura to target creature. Activate only as a sorcery
 * and only once each turn.
 *
 * The three restrictions are three statics over `GroupFilter.attachedCreature()`: [CantAttack],
 * [CantBlock], and the [AbilityFlag.CANT_TRANSFORM] flag. The transform prohibition is honored in
 * the engine's single shared transform-in-place implementation, so it stops *every* cause of a
 * transform — a `TransformEffect` one-shot, an activated/triggered transform ability, and the
 * daybound/nightbound day-change flips a werewolf would otherwise get for free. Per the ruling,
 * such an ability can still be activated or triggered and its other effects still happen; only
 * the flip itself does nothing.
 *
 * The move ability re-attaches the Aura itself, so it uses `Effects.AttachEquipment` with
 * `EffectTarget.ContextTarget(0)` — that effect is attachment-generic (it rewrites
 * `AttachedToComponent` / `AttachmentsComponent`), not Equipment-specific, and it detaches from the
 * old host first. The Aura stays under its controller's control while enchanting an opponent's
 * creature, so only that controller ever sees the activation.
 */
val BoundByMoonsilver = card("Bound by Moonsilver") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack, block, or transform.\n" +
        "Sacrifice another permanent: Attach this Aura to target creature. Activate only as a " +
        "sorcery and only once each turn."

    auraTarget = Targets.Creature

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.CANT_TRANSFORM.name, GroupFilter.attachedCreature())
    }

    activatedAbility {
        cost = Costs.SacrificeAnother()
        target = Targets.Creature
        effect = Effects.AttachEquipment(EffectTarget.ContextTarget(0))
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "Sacrifice another permanent: Attach this Aura to target creature"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bdf107d-eb67-421c-a7a9-b3e62e03f766.jpg?1783937825"
        ruling(
            "2016-04-08",
            "Activated and triggered abilities of the enchanted creature that would cause it to " +
                "transform can still be activated or triggered. If those abilities have any other " +
                "effects, those effects will happen."
        )
        ruling(
            "2016-04-08",
            "You control Bound by Moonsilver even while it enchants an opponent's creature. Only " +
                "you can activate its last ability."
        )
    }
}
