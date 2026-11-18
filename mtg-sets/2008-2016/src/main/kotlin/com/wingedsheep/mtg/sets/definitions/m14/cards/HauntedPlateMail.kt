package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Haunted Plate Mail
 * {4} · Artifact — Equipment
 * Equipped creature gets +4/+4.
 * {0}: Until end of turn, this permanent becomes a 4/4 Spirit artifact creature that's no longer an
 * Equipment. Activate only if you control no creatures.
 * Equip {4}
 *
 * The animation is Dancing Sword's shape with an end-of-turn duration: a layer-4 `removeSubtypes`
 * drops Equipment (and adds Spirit) only until end of turn, then the Plate Mail is an Equipment again.
 * While it is a creature it can't stay attached — the attachment state-based action unattaches an
 * Equipment that is itself a creature — and an Equip activated meanwhile leaves it unattached
 * (2014-07-18 ruling). "Activate only if you control no creatures" is checked only on activation;
 * gaining a creature in response doesn't stop the ability (2014-07-18 ruling).
 */
val HauntedPlateMail = card("Haunted Plate Mail") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +4/+4.\n" +
        "{0}: Until end of turn, this permanent becomes a 4/4 Spirit artifact creature that's no longer an " +
        "Equipment. Activate only if you control no creatures.\n" +
        "Equip {4} ({4}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+4, +4, Filters.EquippedCreature)
    }

    activatedAbility {
        cost = Costs.Free
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            creatureTypes = setOf("Spirit"),
            removeSubtypes = setOf("Equipment"),
            duration = Duration.EndOfTurn,
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.YouControl(GameObjectFilter.Creature, negate = true))
        )
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2dc1e07-7894-4f22-936d-bf5df3f8d5a5.jpg?1783939895"
        ruling(
            "2014-07-18",
            "Although you must control no creatures to activate the \"animation\" ability, having a creature " +
                "somehow come under your control in response to that ability won't stop it from resolving.",
        )
        ruling(
            "2014-07-18",
            "You can activate the equip ability of Haunted Plate Mail while it isn't an Equipment, but the " +
                "ability will have no effect. Haunted Plate Mail won't become attached to any creature.",
        )
    }
}
