package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tethmos High Priest — Journey into Nyx #29 (canonical printing)
 * {2}{W}
 * Creature — Cat Cleric
 * 2/3
 * Heroic — Whenever you cast a spell that targets this creature, return target creature card with
 * mana value 2 or less from your graveyard to the battlefield.
 *
 * Heroic is the `TargetsSource` cast trigger (Elite Skirmisher); the reanimation is Yathan
 * Roadwatcher's targeted `PutOntoBattlefield` over a mana-value-capped graveyard creature filter.
 */
val TethmosHighPriest = card("Tethmos High Priest") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Cleric"
    power = 2
    toughness = 3
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, return target creature card " +
        "with mana value 2 or less from your graveyard to the battlefield."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        val creature = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(2))
        effect = Effects.PutOntoBattlefield(creature)
        description = "Heroic — Whenever you cast a spell that targets this creature, return target creature " +
            "card with mana value 2 or less from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Mark Zug"
        flavorText = "\"Death is tyranny. Like all tyranny, it must be opposed.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d901b9d7-6af9-40ce-afb7-d3c9f9143c18.jpg?1783939452"
        ruling("2018-12-07", "If a card in a player's graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
