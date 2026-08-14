package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Biorganic Carapace
 * {2}{W}{U}
 * Artifact — Equipment
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +2/+2 and has "Whenever this creature deals combat damage to a player,
 * draw a card for each modified creature you control." (Equipment, Auras you control, and counters
 * are modifications.)
 * Equip {2}
 */
val BiorganicCarapace = card("Biorganic Carapace") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +2/+2 and has \"Whenever this creature deals combat damage to a player, " +
        "draw a card for each modified creature you control.\" " +
        "(Equipment, Auras you control, and counters are modifications.)\n" +
        "Equip {2}"

    // "modified creature you control" — CR 700.4: a permanent with a counter, an Aura you control,
    // or an Equipment attached to it.
    val modifiedCreatureYouControl = GameObjectFilter.Creature.youControl().let {
        it.copy(statePredicates = it.statePredicates + StatePredicate.IsModified)
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl)
        )
        effect = Effects.AttachEquipment(creature)
    }

    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.DealsCombatDamageToPlayer.event,
                binding = Triggers.DealsCombatDamageToPlayer.binding,
                effect = Effects.DrawCards(
                    DynamicAmounts.battlefield(Player.You, modifiedCreatureYouControl).count()
                ),
            ),
            filter = Filters.EquippedCreature,
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9658fdab-9702-4e13-bc53-01a25a2ed41a.jpg?1783905320"
    }
}
