package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Simian Sling — Kamigawa: Neon Dynasty #163 (canonical printing)
 * {R} · Artifact Creature — Equipment Monkey · 1/1
 *
 * Equipped creature gets +1/+1.
 * Whenever this creature or equipped creature becomes blocked, it deals 1 damage to defending player.
 * Reconfigure {2}
 *
 * The or-joined trigger is two triggered abilities (Chainflail Centipede shape): one SELF-bound for
 * the unattached Sling, one ATTACHED-bound for the equipped creature. "It" is the blocked creature —
 * on the attached trigger the equipped creature deals the damage ([EffectTarget.TriggeringEntity]),
 * not the Sling (2022-02-18 ruling), and the defending player is read off that attacker.
 */
val SimianSling = card("Simian Sling") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Equipment Monkey"
    power = 1
    toughness = 1
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever this creature or equipped creature becomes blocked, it deals 1 damage to defending player.\n" +
        "Reconfigure {2} ({2}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.DefendingPlayer))
        description = "Whenever this creature becomes blocked, it deals 1 damage to defending player."
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

    reconfigure("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Nicholas Elias"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37a00f95-d563-4d51-a5f2-af139261921a.jpg?1783923860"
        ruling(
            "2022-02-18",
            "If Simian Sling becomes blocked, Simian Sling deals the damage. If the equipped creature becomes " +
                "blocked, that creature deals the damage, not Simian Sling.",
        )
    }
}
