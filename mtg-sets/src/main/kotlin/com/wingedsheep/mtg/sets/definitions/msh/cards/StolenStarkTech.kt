package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Stolen Stark Tech
 * {1}{B}
 * Artifact — Equipment
 *
 * Flash
 * When this Equipment enters, attach it to target creature you control.
 * That creature gains indestructible until end of turn.
 * Equipped creature gets +1/+0.
 * Equip {1}
 *
 * Implementation notes:
 * - The ETB trigger targets once and does both things to that same creature: the bound target
 *   is reused by [Effects.AttachEquipment] and the indestructible grant, so the creature keeps
 *   indestructible even if the attach is later undone.
 * - Flash lets it be cast at instant speed as a combat trick; the equip ability itself remains
 *   sorcery-speed (handled by the [equipAbility] facade).
 */
val StolenStarkTech = card("Stolen Stark Tech") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\n" +
        "When this Equipment enters, attach it to target creature you control. That creature " +
        "gains indestructible until end of turn.\n" +
        "Equipped creature gets +1/+0.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
            .then(Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature, Duration.EndOfTurn))
    }

    staticAbility {
        ability = ModifyStats(+1, 0, Filters.EquippedCreature)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Lixin Yin"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db4557ad-4141-4dae-870e-a587506f4914.jpg?1783902937"
    }
}
