package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ancestral Katana — Kamigawa: Neon Dynasty #1 (canonical printing)
 * {1}{W} · Artifact — Equipment
 *
 * Whenever a Samurai or Warrior you control attacks alone, you may pay {1}. When you do, attach
 * this Equipment to it.
 * Equipped creature gets +2/+1.
 * Equip {3}
 *
 * Paying {1} sets off a reflexive trigger (players may respond to it); that reflexive half inherits
 * the attack trigger's record, so [EffectTarget.TriggeringEntity] still names the lone attacker.
 */
val AncestralKatana = card("Ancestral Katana") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever a Samurai or Warrior you control attacks alone, you may pay {1}. When you do, " +
        "attach this Equipment to it.\n" +
        "Equipped creature gets +2/+1.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR)).youControl()
        ).attacks(setOf(AttackPredicate.Alone))
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}"),
            optional = true,
            reflexiveEffect = Effects.AttachEquipment(EffectTarget.TriggeringEntity),
            descriptionOverride = "attach this Equipment to that creature",
        )
        description = "Whenever a Samurai or Warrior you control attacks alone, you may pay {1}. " +
            "When you do, attach this Equipment to it."
    }

    staticAbility {
        ability = ModifyStats(2, 1)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9b1ed43-ee6c-43a2-ba94-5bf71c63e070.jpg?1783923929"
        ruling(
            "2022-02-18",
            "A creature attacks alone if it's the only creature declared as an attacker during the declare " +
                "attackers step. For example, Ancestral Katana's triggered ability won't trigger if you attack " +
                "with multiple creatures and all but one of them are removed from combat."
        )
        ruling(
            "2022-02-18",
            "If you choose to pay {1} as Ancestral Katana's ability resolves, a second triggered ability " +
                "triggers and players may respond to it. Then, as that second ability resolves, Ancestral " +
                "Katana becomes attached to the creature that attacked alone."
        )
    }
}
