package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Impending Doom
 * {2}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +3/+3 and attacks each combat if able.
 * When enchanted creature dies, this Aura deals 3 damage to that creature's controller.
 *
 * "That creature's controller" is the dying creature's last-known controller, which
 * [EffectTarget.ControllerOfTriggeringEntity] reads off the zone-change trigger context.
 */
val ImpendingDoom = card("Impending Doom") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +3/+3 and attacks each combat if able.\n" +
        "When enchanted creature dies, this Aura deals 3 damage to that creature's controller."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 3)
    }
    staticAbility {
        ability = MustAttack(GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.DealDamage(3, EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Zoltan Boros"
        flavorText = "\"Feast, for your next meal may be your last!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5681db91-5dab-41e6-96f2-275ec9495e5b.jpg?1783931550"
        ruling(
            "2020-01-24",
            "If the enchanted creature can't attack for any reason (such as being tapped or having " +
                "come under that player's control that turn), then it doesn't attack. If there's a cost " +
                "associated with having it attack, the player isn't forced to pay that cost, so it " +
                "doesn't have to attack in that case either."
        )
    }
}
