package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Raven's Run Dragoon
 * {2}{G/W}{G/W}
 * Creature — Elf Knight
 * 3 / 3
 *
 * This creature can't be blocked by black creatures.
 *
 * - The evasion is a single [CantBeBlockedBy] static over a coloured *creature* filter, evaluated
 *   against projected state so a blocker's current colour decides, not its printed one.
 * - The hybrid cost `{G/W}` goes in `manaCost` verbatim; mana value (4) is derived by the parser.
 */
val RavensRunDragoon = card("Raven's Run Dragoon") {
    manaCost = "{2}{G/W}{G/W}"
    typeLine = "Creature — Elf Knight"
    power = 3
    toughness = 3
    oracleText = "This creature can't be blocked by black creatures."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.BLACK))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "235"
        artist = "Daren Bader"
        flavorText = "\"I have a gift. The ability to sense encroaching darkness has saved many lives. And yet constantly feeling the force of so much ugliness is a terrible burden.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b868da85-9d19-4407-a0c3-fc3b2b237cda.jpg?1783942716"
    }
}
