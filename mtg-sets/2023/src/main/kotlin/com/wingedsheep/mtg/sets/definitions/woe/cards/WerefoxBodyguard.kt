package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Werefox Bodyguard
 * {1}{W}{W}
 * Creature — Elf Fox Knight (2/2)
 *
 * Flash
 * When this creature enters, exile up to one other target non-Fox creature until this creature
 * leaves the battlefield.
 * {1}{W}, Sacrifice this creature: You gain 2 life.
 *
 * O-Ring-style linked exile: the ETB exiles the target ("up to one" → optional) via
 * [Effects.ExileUntilLeaves], and a companion `Triggers.self.leaves()` trigger returns the
 * exiled card with [Effects.ReturnLinkedExileUnderOwnersControl]. The target filter is
 * `Creature.notSubtype(Fox)` with `excludeSelf` (Werefox is itself a Fox, and the oracle says
 * "other"). The sacrifice ability lets its owner cash it in for 2 life before an opponent can
 * remove it to free the exiled creature.
 */
val WerefoxBodyguard = card("Werefox Bodyguard") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elf Fox Knight"
    power = 2
    toughness = 2
    oracleText = "Flash\n" +
        "When this creature enters, exile up to one other target non-Fox creature until this " +
        "creature leaves the battlefield.\n" +
        "{1}{W}, Sacrifice this creature: You gain 2 life."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype("Fox")), excludeSelf = true),
            optional = true,
        )
        effect = Effects.ExileUntilLeaves(creature)
        description = "When this creature enters, exile up to one other target non-Fox creature " +
            "until this creature leaves the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.SacrificeSelf)
        effect = Effects.GainLife(2)
        description = "{1}{W}, Sacrifice this creature: You gain 2 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4494dfa1-1343-417e-b0c5-2b096442dd0e.jpg?1783915124"
    }
}
