package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elvish Archivist
 * {1}{G}
 * Creature — Elf Artificer
 * 0/1
 *
 * Whenever one or more artifacts you control enter, put two +1/+1 counters on this creature.
 * This ability triggers only once each turn.
 * Whenever one or more enchantments you control enter, draw a card.
 * This ability triggers only once each turn.
 *
 * Two independent batching triggers (CR 603.3b): each fires at most once per event batch no matter how
 * many permanents entered together, and `oncePerTurn` then caps each at one firing per turn. The two
 * caps are tracked separately — a turn where an artifact and an enchantment both enter gets *both* the
 * counters and the card. An artifact **enchantment** entering satisfies both filters and likewise
 * triggers both abilities once each.
 *
 * The default controller scope on [Triggers.OneOrMorePermanentsEnter] is "you control", which is what
 * the oracle says; an opponent's artifacts entering do nothing here.
 *
 * `excludeSource` is deliberately left at its default: Elvish Archivist is a creature, not an artifact
 * or enchantment, so it can never be a member of either batch and there is nothing to exclude. (It
 * *could* become one — an animating effect making it an artifact creature would let its own re-entry
 * count — and that is correct under the oracle wording, which has no "other".)
 *
 * A 0/1 body that never grew would be a blank; the counters are real +1/+1 counters in layer 7d, so a
 * later "base power and toughness" effect still stacks on top of them rather than erasing them.
 */
val ElvishArchivist = card("Elvish Archivist") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Artificer"
    oracleText = "Whenever one or more artifacts you control enter, put two +1/+1 counters on this " +
        "creature. This ability triggers only once each turn.\n" +
        "Whenever one or more enchantments you control enter, draw a card. This ability triggers " +
        "only once each turn."
    power = 0
    toughness = 1

    triggeredAbility {
        trigger = Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Artifact)
        oncePerTurn = true
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Whenever one or more artifacts you control enter, put two +1/+1 counters on " +
            "this creature. This ability triggers only once each turn."
    }

    triggeredAbility {
        trigger = Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Enchantment)
        oncePerTurn = true
        effect = Effects.DrawCards(1)
        description = "Whenever one or more enchantments you control enter, draw a card. This " +
            "ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0670dbf0-b150-4e8d-bb40-f768b2f06fe5.jpg?1783915083"
    }
}
