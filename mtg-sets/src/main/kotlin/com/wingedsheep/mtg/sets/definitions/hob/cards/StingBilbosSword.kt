package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sting, Bilbo's Sword
 * {2}
 * Legendary Artifact — Equipment
 *
 * Flash
 * When Sting enters, put a hone counter on Sting for each creature target opponent controls.
 * Attach Sting to up to one target creature you control.
 * Equip {3}
 *
 * The hone counters carry the whole payoff: CR 122.1j gives the equipped creature +1/+0 per hone
 * counter on the Equipment, so Sting needs no `ModifyStats` of its own — see [Counters.HONE]. That
 * also means the counters are *sticky*: they are counted when the ETB resolves and stay at that
 * number afterwards, so a later board wipe on the opponent's side doesn't shrink Sting.
 *
 * Target order is load-bearing. The opponent is targeted first and the creature second because the
 * optional slot has to come last — cast-time target slots are fixed-width and positional, so an
 * "up to one" ahead of a required target would shift the required one's index when declined.
 * `Player.TargetOpponent` resolves to the first *player* target in the context rather than
 * positional slot 0, so the count reads the opponent even with the creature target alongside it.
 *
 * "Up to one target creature" is genuinely optional (`TargetCreature(optional = true)`), which
 * matters for a flashed-in Sting held up during an opponent's turn: declining leaves it unattached
 * rather than forcing it onto a creature, and it still gets its counters.
 */
val StingBilbosSword = card("Sting, Bilbo's Sword") {
    manaCost = "{2}"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Flash\n" +
        "When Sting enters, put a hone counter on Sting for each creature target opponent " +
        "controls. Attach Sting to up to one target creature you control. (Each hone counter on " +
        "an Equipment grants +1/+0 to equipped creature.)\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target("target opponent", TargetOpponent())
        val creature = target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl),
        )
        effect = Effects.AddDynamicCounters(
            Counters.HONE,
            DynamicAmount.AggregateBattlefield(Player.TargetOpponent, GameObjectFilter.Creature),
            EffectTarget.Self,
        ).then(Effects.AttachEquipment(creature))
        description = "When Sting enters, put a hone counter on Sting for each creature target " +
            "opponent controls. Attach Sting to up to one target creature you control."
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "178"
        artist = "Tomas Duchek"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6a8d698-c454-42c4-ad4e-9a7625d5569f.jpg?1784377051"
    }
}
