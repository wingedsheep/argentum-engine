package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressHexproofForGroup
import com.wingedsheep.sdk.scripting.SuppressWardForGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nowhere to Run
 * {1}{B}
 * Enchantment
 *
 * Flash
 * When this enchantment enters, target creature an opponent controls gets -3/-3 until end of turn.
 * Creatures your opponents control can be the targets of spells and abilities as though they didn't
 * have hexproof. Ward abilities of those creatures don't trigger.
 *
 * Ruling: Ward is suppressed regardless of whether the creature has hexproof.
 * Ruling: If Nowhere to Run leaves the battlefield, hexproof and ward are immediately restored —
 * any spell already targeting an opponent's creature that had hexproof fizzles if the target
 * regains hexproof and becomes illegal.
 */
val NowhereToRun = card("Nowhere to Run") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Flash\nWhen this enchantment enters, target creature an opponent controls gets -3/-3 until end of turn.\nCreatures your opponents control can be the targets of spells and abilities as though they didn't have hexproof. Ward abilities of those creatures don't trigger."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target creature", Targets.CreatureOpponentControls)
        effect = Effects.ModifyStats(-3, -3, t)
        description = "When this enchantment enters, target creature an opponent controls gets -3/-3 until end of turn."
    }

    staticAbility {
        ability = SuppressHexproofForGroup(filter = GroupFilter.AllCreaturesOpponentsControl)
    }

    staticAbility {
        ability = SuppressWardForGroup(filter = GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Jodie Muir"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fee60e9d-9ee7-444a-88f3-c1929e1888fb.jpg?1726286262"
        ruling("2024-09-20", "If a spell or ability you control targets a creature an opponent controls with hexproof and Nowhere to Run leaves the battlefield while that spell or ability is on the stack, that creature becomes an illegal target for that spell or ability. This includes Nowhere to Run's own triggered ability.")
        ruling("2024-09-20", "If a spell or ability targeting a creature an opponent controls with a ward ability is on the stack, causing Nowhere to Run to leave the battlefield won't cause that ward ability to trigger.")
        ruling("2024-09-20", "Ward abilities of creatures your opponents control won't trigger as long as Nowhere to Run is on the battlefield. It doesn't matter whether or not they have hexproof.")
    }
}
