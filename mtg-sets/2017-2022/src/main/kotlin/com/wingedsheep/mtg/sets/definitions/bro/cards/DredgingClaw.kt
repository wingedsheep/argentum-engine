package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dredging Claw
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +1/+0 and has menace.
 * Whenever a creature enters from your graveyard, you may attach this Equipment to it.
 * Equip {1}{B}
 *
 * "From your graveyard" is a creature *owned by you* entering from a graveyard — a card only ever
 * sits in its owner's graveyard — so it fires whoever controls the returned creature.
 */
val DredgingClaw = card("Dredging Claw") {
    manaCost = "{2}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has menace. (It can't be blocked except by two or more creatures.)\n" +
        "Whenever a creature enters from your graveyard, you may attach this Equipment to it.\n" +
        "Equip {1}{B} ({1}{B}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.ownedByYou()).enters(from = Zone.GRAVEYARD)
        optional = true
        effect = Effects.AttachEquipment(EffectTarget.TriggeringEntity)
    }

    equipAbility("{1}{B}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8e133a2-631f-43f6-9fdd-e542d4d15e10.jpg?1783920080"
    }
}
