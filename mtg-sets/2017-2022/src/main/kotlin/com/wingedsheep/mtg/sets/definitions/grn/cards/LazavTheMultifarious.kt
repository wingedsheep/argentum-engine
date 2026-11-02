package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lazav, the Multifarious — Guilds of Ravnica #184 (canonical printing)
 * {U}{B} · Legendary Creature — Shapeshifter · 1/3
 *
 * When Lazav enters, surveil 1.
 * {X}: Lazav becomes a copy of target creature card in your graveyard with mana value X, except its
 * name is Lazav, the Multifarious, it's legendary in addition to its other types, and it has this
 * ability.
 *
 * The Likeness Looter shape: an `{X}` activation threading X into the target filter via
 * `manaValueEqualsX()`, `sourceFromAnyZone` so the copy source stays in the graveyard, and
 * `affected = Self`. The name and legendary exceptions ride [CopyExceptions] (CR 707.9), and
 * `retainActivatingAbility` re-grants this ability so Lazav can be re-aimed. No sorcery-speed
 * restriction — unlike Likeness Looter, Lazav can shift at instant speed.
 */
val LazavTheMultifarious = card("Lazav, the Multifarious") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Shapeshifter"
    power = 1
    toughness = 3
    oracleText = "When Lazav enters, surveil 1. (Look at the top card of your library. You may put it " +
        "into your graveyard.)\n" +
        "{X}: Lazav becomes a copy of target creature card in your graveyard with mana value X, except " +
        "its name is Lazav, the Multifarious, it's legendary in addition to its other types, and it " +
        "has this ability."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.surveil(1)
    }

    activatedAbility {
        cost = Costs.Mana("{X}")
        val creatureCard = target(
            TargetFilter(GameObjectFilter.Creature.ownedByYou().manaValueEqualsX(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = creatureCard,
            affected = EffectTarget.Self,
            sourceFromAnyZone = true,
            duration = Duration.Permanent,
            exceptions = CopyExceptions(
                nameOverride = "Lazav, the Multifarious",
                addedSupertypes = setOf(Supertype.LEGENDARY),
            ),
            retainActivatingAbility = true,
        )
        description = "{X}: Lazav becomes a copy of target creature card in your graveyard with mana " +
            "value X, except its name is Lazav, the Multifarious, it's legendary in addition to its " +
            "other types, and it has this ability."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "184"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24c1e710-f61c-4dd6-b620-6d2e1ecab689.jpg?1783934131"
        ruling(
            "2018-10-05",
            "Lazav copies exactly what was printed on the original card and nothing else, except the " +
                "characteristics it specifically modifies. It doesn't copy any information about the " +
                "object the card was before it was put into your graveyard."
        )
        ruling(
            "2018-10-05",
            "Any effects that applied to Lazav before it becomes a copy of another card will continue " +
                "to apply once it's become a copy. The same is true of any counters that are on Lazav."
        )
        ruling("2018-10-05", "If a card in your graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling(
            "2018-10-05",
            "Because Lazav isn't entering the battlefield when it becomes a copy of a card, any \"When " +
                "this enters the battlefield\" or \"This enters the battlefield with\" abilities of the " +
                "copied card won't apply."
        )
        ruling(
            "2018-10-05",
            "If the copied card has an ability that can be activated only once each turn, copying that " +
                "card a second time will allow you to activate the new instance of that ability."
        )
    }
}
