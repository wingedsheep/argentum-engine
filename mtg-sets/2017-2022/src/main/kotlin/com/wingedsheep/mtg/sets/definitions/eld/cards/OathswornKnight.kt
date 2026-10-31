package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter

/**
 * Oathsworn Knight
 * {1}{B}{B}
 * Creature — Human Knight
 * 0/0
 * This creature enters with four +1/+1 counters on it.
 * This creature attacks each combat if able.
 * If damage would be dealt to this creature while it has a +1/+1 counter on it, prevent that
 * damage and remove a +1/+1 counter from it.
 *
 * The last line is [PreventDamageByRemovingCounter] with `requiresCounter = true` ("while it has
 * a +1/+1 counter on it", as Magma Pummeler) and the default one-counter removal (Unbreathing
 * Horde's "a +1/+1 counter", one per damage event however much damage is prevented).
 */
val OathswornKnight = card("Oathsworn Knight") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    power = 0
    toughness = 0
    oracleText = "This creature enters with four +1/+1 counters on it.\n" +
        "This creature attacks each combat if able.\n" +
        "If damage would be dealt to this creature while it has a +1/+1 counter on it, prevent " +
        "that damage and remove a +1/+1 counter from it."

    replacementEffect(EntersWithCounters(count = 4, selfOnly = true))

    staticAbility {
        ability = MustAttack()
    }

    replacementEffect(PreventDamageByRemovingCounter(requiresCounter = true))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/9173ffda-1d3b-4dab-8dcb-de44717de464.jpg?1783932637"
        ruling("2019-10-04", "Oathsworn Knight's last ability removes only one counter from it each time it would be dealt damage, not one counter per 1 damage prevented.")
        ruling("2019-10-04", "If multiple sources would deal damage to Oathsworn Knight simultaneously (for example, multiple creatures blocking it), its last ability applies once. All of that damage is prevented, and only one counter is removed.")
        ruling("2019-10-04", "If damage that can't be prevented is dealt to Oathsworn Knight, one counter is still removed from it.")
    }
}
