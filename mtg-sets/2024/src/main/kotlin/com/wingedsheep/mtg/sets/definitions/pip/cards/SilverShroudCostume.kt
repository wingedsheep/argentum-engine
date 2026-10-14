package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silver Shroud Costume
 * {2}
 * Artifact — Equipment
 *
 * Flash
 * When this Equipment enters, attach it to target creature you control. That creature gains shroud
 * until end of turn. (It can't be the target of spells or abilities.)
 * Equipped creature can't be blocked.
 * Equip {3}
 *
 * The Bramble Armor ETB-attach idiom plus an end-of-turn shroud grant on the same target. If the
 * target is illegal on resolution the whole trigger fizzles, so neither half happens.
 */
val SilverShroudCostume = card("Silver Shroud Costume") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\n" +
        "When this Equipment enters, attach it to target creature you control. That creature gains shroud " +
        "until end of turn. (It can't be the target of spells or abilities.)\n" +
        "Equipped creature can't be blocked.\n" +
        "Equip {3}"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t) then Effects.GrantKeyword(Keyword.SHROUD, t)
    }

    staticAbility {
        ability = CantBeBlocked(GroupFilter.attachedCreature())
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Zuzanna Wużyk"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f21fba59-330b-4daf-bf77-6dd817327a64.jpg?1783912376"
    }
}
