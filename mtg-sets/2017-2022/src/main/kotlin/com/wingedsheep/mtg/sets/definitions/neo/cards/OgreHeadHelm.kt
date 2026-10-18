package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ogre-Head Helm — Kamigawa: Neon Dynasty #155 (canonical printing)
 * {1}{R} · Artifact Creature — Equipment Ogre · 2/2
 *
 * Equipped creature gets +2/+2.
 * Whenever this creature or equipped creature deals combat damage to a player, you may sacrifice it.
 * If you do, discard your hand, then draw three cards.
 * Reconfigure {3}
 *
 * The or-joined trigger is two triggered abilities (Acquisition Octopus shape): one SELF-bound for
 * the unattached Helm, one ATTACHED-bound for the equipped creature. On the attached trigger "it" is
 * the creature that dealt the damage — [EffectTarget.DamageSource], fixed when the trigger fires —
 * so the equipped creature is the one sacrificed, not the Helm (2022-02-18 ruling), even if the Helm
 * is moved or leaves before the trigger resolves.
 */
val OgreHeadHelm = card("Ogre-Head Helm") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Equipment Ogre"
    power = 2
    toughness = 2
    oracleText = "Equipped creature gets +2/+2.\n" +
        "Whenever this creature or equipped creature deals combat damage to a player, you may sacrifice it. " +
        "If you do, discard your hand, then draw three cards.\n" +
        "Reconfigure {3} ({3}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        optional = true
        effect = Effects.IfYouDo(
            action = Effects.SacrificeTarget(EffectTarget.Self),
            then = Patterns.Hand.discardHand() then Effects.DrawCards(3),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "Whenever this creature deals combat damage to a player, you may sacrifice it. " +
            "If you do, discard your hand, then draw three cards."
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        optional = true
        effect = Effects.IfYouDo(
            action = Effects.SacrificeTarget(EffectTarget.DamageSource),
            then = Patterns.Hand.discardHand() then Effects.DrawCards(3),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "Whenever equipped creature deals combat damage to a player, you may sacrifice it. " +
            "If you do, discard your hand, then draw three cards."
    }

    reconfigure("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a0d5f7d-4e8a-42ad-8875-610004bd9796.jpg?1783923863"
        ruling(
            "2022-02-18",
            "If the equipped creature deals combat damage, you may sacrifice the equipped creature, not " +
                "Ogre-Head Helm.",
        )
    }
}
