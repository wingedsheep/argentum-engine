package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chainflail Centipede — Kamigawa: Neon Dynasty #90 (canonical printing)
 * {2}{B} · Artifact Creature — Equipment Insect · 2/2
 *
 * Whenever this creature or equipped creature attacks, it gets +2/+0 until end of turn.
 * Reconfigure {2}
 *
 * The or-joined trigger is two triggered abilities (Acquisition Octopus shape): one SELF-bound for
 * the unattached Centipede, one ATTACHED-bound for the equipped creature. On the attached trigger
 * "it" is the attacking creature — [EffectTarget.TriggeringEntity] — so the pump still lands on it
 * even if the Centipede is unattached before the trigger resolves (2022-02-18 ruling).
 */
val ChainflailCentipede = card("Chainflail Centipede") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Equipment Insect"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or equipped creature attacks, it gets +2/+0 until end of turn.\n" +
        "Reconfigure {2} ({2}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.ModifyStats(2, 0, EffectTarget.TriggeringEntity)
    }

    reconfigure("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24b8efe0-2f7b-4ada-a1b9-9c5f9e6a114d.jpg?1783923888"
        ruling(
            "2022-02-18",
            "If the equipped creature is the one that attacks, it gets +2/+0 until end of turn, not " +
                "Chainflail Centipede. This is true even if Chainflail Centipede is no longer attached " +
                "to it by the time the ability resolves.",
        )
    }
}
