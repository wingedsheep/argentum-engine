package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tormentor's Helm — Kaldheim #155
 * {R} · Artifact — Equipment
 *
 * Equipped creature gets +1/+1.
 * Whenever equipped creature becomes blocked, it deals 1 damage to defending player.
 * Equip {1}
 *
 * "It" is the equipped creature, so the blocked attacker is the damage source
 * ([EffectTarget.TriggeringEntity]), and the defending player is read off that attacker
 * (Simian Sling's attached trigger, same shape).
 */
val TormentorsHelm = card("Tormentor's Helm") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever equipped creature becomes blocked, it deals 1 damage to defending player.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.becomesBlocked()
        effect = Effects.DealDamage(
            1,
            EffectTarget.PlayerRef(Player.DefendingPlayer),
            damageSource = EffectTarget.TriggeringEntity,
        )
        description = "Whenever equipped creature becomes blocked, it deals 1 damage to defending player."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2318d4f8-309e-4645-b6d1-46572edd6996.jpg?1783928221"
        ruling(
            "2021-02-05",
            "The triggered ability triggers only once if the equipped creature becomes blocked, no matter how " +
                "many creatures blocked it.",
        )
    }
}
