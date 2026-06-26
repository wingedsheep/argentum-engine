package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.jobSelect
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sage's Nouliths
 * {1}{U}
 * Artifact — Equipment
 * Job select (When this Equipment enters, create a 1/1 colorless Hero creature token,
 *   then attach this to it.)
 * Equipped creature gets +1/+0, has "Whenever this creature attacks, untap target attacking
 *   creature," and is a Cleric in addition to its other types.
 * Hagneia — Equip {3}
 *
 * "Hagneia" is a Final Fantasy ability word that flavors the Equip ability — it carries no rules
 * meaning, so it lives only in the oracle text; mechanically the ability is a plain Equip {3}.
 * The granted "Whenever this creature attacks, untap target attacking creature" ability lives on
 * the equipped creature (GrantTriggeredAbility over the attached-creature filter, SELF binding),
 * mirroring Web-Shooters' "Whenever this creature attacks, tap target …" shape.
 */
val SagesNouliths = card("Sage's Nouliths") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "Job select (When this Equipment enters, create a 1/1 colorless Hero creature token, then attach this to it.)\n" +
        "Equipped creature gets +1/+0, has \"Whenever this creature attacks, untap target attacking creature,\" and is a Cleric in addition to its other types.\n" +
        "Hagneia — Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    jobSelect()

    staticAbility {
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.Attacks.event,
                binding = Triggers.Attacks.binding,
                effect = Effects.Untap(EffectTarget.ContextTarget(0)),
                targetRequirement = Targets.AttackingCreature,
            ),
            filter = Filters.EquippedCreature,
        )
    }
    staticAbility {
        ability = GrantSubtype("Cleric", Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a12ff7c3-6ae0-4098-9240-ff3fd16a5288.jpg?1748706016"
    }
}
