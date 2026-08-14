package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Mjölnir, Hammer of Thor — Marvel Super Heroes #146
 * {3}{R} · Legendary Artifact — Equipment
 *
 * When Mjölnir enters, it deals 4 damage to up to one target creature.
 * Double all damage equipped creature would deal.
 * Equip worthy {1} (A creature is worthy if it's a legendary non-Villain that's red and/or white.)
 * {2}{R}, Discard this card: It deals 2 damage to each creature.
 *
 * Implementation notes:
 *
 * - **"Double all damage equipped creature would deal"** is a [DoubleDamage] replacement scoped to
 *   [SourceFilter.EquippedCreature] — damage dealt *by* the permanent this Equipment is attached
 *   to, combat or otherwise, to any recipient. The engine's shared damage-source matcher resolves
 *   it from the host's attachment, so it follows the Equipment when it moves.
 * - **"Equip worthy {1}"** is an "Equip [quality]" variant (CR 702.6c): the ability may target
 *   only a creature its activating player controls that has the stated quality. "Worthy" is
 *   defined in this card's own reminder text and appears on no other printed card, so it is spelled
 *   out as a composition of existing predicates rather than becoming an SDK concept — legendary,
 *   not a Villain, and red and/or white (an *or*, not a requirement to be both). Per CR 702.6c the
 *   quality restricts *targeting* only: a creature that stops being worthy after the fact stays
 *   equipped; the Equipment comes off only under CR 704.5n.
 * - **"{2}{R}, Discard this card"** functions from hand — the standard [Costs.DiscardSelf] +
 *   `activateFromZone = Zone.HAND` pattern (Trumpeting Carnosaur). "It" is Mjölnir itself, which
 *   is the resolving ability's source, so the damage needs no `damageSource` override. The sweep
 *   is [Effects.ForEachInGroup] over every creature, as Slagstorm does.
 */
val MjolnirHammerOfThor = card("Mjölnir, Hammer of Thor") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "When Mjölnir enters, it deals 4 damage to up to one target creature.\n" +
        "Double all damage equipped creature would deal.\n" +
        "Equip worthy {1} (A creature is worthy if it's a legendary non-Villain that's red " +
        "and/or white.)\n" +
        "{2}{R}, Discard this card: It deals 2 damage to each creature."

    // When Mjölnir enters, it deals 4 damage to up to one target creature.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "up to one target creature",
            TargetCreature(optional = true, filter = TargetFilter.Creature)
        )
        effect = Effects.DealDamage(4, creature)
    }

    // Double all damage equipped creature would deal.
    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(source = SourceFilter.EquippedCreature)
        )
    )

    // Equip worthy {1} — a legendary non-Villain that's red and/or white, that you control.
    equipAbility(
        "{1}",
        quality = "worthy",
        targetFilter = TargetFilter(
            GameObjectFilter.Creature
                .legendary()
                .notSubtype(Subtype.VILLAIN)
                .withAnyColor(Color.RED, Color.WHITE)
                .youControl()
        ),
    )

    // {2}{R}, Discard this card: It deals 2 damage to each creature.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.DiscardSelf)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            DealDamageEffect(2, EffectTarget.Self)
        )
        activateFromZone = Zone.HAND
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "146"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0c7f566-5351-44e3-a346-b84b0eb10209.jpg?1783902926"
    }
}
