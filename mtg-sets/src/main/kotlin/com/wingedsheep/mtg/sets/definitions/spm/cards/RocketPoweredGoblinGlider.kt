package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Rocket-Powered Goblin Glider — Marvel's Spider-Man #172
 * {3} · Artifact — Equipment
 *
 * When this Equipment enters, if it was cast from your graveyard, attach it to target creature
 * you control.
 * Equipped creature gets +2/+0 and has flying and haste.
 * Equip {2}
 * Mayhem {2}
 *
 * The ETB attach is an intervening-'if' (CR 603.4) gated on [Conditions.WasCastFromGraveyard] — the
 * only way to reach the graveyard-cast is this card's Mayhem ability (CR 702.187), which records
 * `castFromZone = GRAVEYARD`.
 */
val RocketPoweredGoblinGlider = card("Rocket-Powered Goblin Glider") {
    manaCost = "{3}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, if it was cast from your graveyard, attach it to target creature you control.\nEquipped creature gets +2/+0 and has flying and haste.\nEquip {2}\nMayhem {2}"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasCastFromGraveyard
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
        description = "When this Equipment enters, if it was cast from your graveyard, attach it " +
            "to target creature you control."
    }

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, Filters.EquippedCreature)
    }

    equipAbility("{2}")
    mayhem("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Pavel Kolomeyets"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6c39232-72cc-4363-83d0-b5873f14f231.jpg?1783905302"
    }
}
