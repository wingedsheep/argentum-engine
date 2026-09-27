package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Beamtown Beatstick — March of the Machine #131
 * {R} · Artifact — Equipment
 *
 * Equipped creature gets +1/+0 and has menace.
 * Whenever equipped creature deals combat damage to a player or battle, create a Treasure token.
 * Equip {2}
 *
 */
val BeamtownBeatstick = card("Beamtown Beatstick") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has menace. (It can't be blocked except by two or more creatures.)\n" +
        "Whenever equipped creature deals combat damage to a player or battle, create a Treasure token.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+1, 0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
        effect = Effects.CreateTreasure()
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Konstantin Porubov"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2443cb94-b27e-4a96-93cb-ac7880149bcc.jpg?1783916998"
    }
}
