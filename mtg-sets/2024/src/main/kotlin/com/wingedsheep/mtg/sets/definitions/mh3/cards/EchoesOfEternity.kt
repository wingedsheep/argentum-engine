package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Echoes of Eternity — Modern Horizons 3 #4 (rare)
 * {3}{C}{C}{C} · Kindred Enchantment — Eldrazi
 *
 * If a triggered ability of a colorless spell you control or another colorless permanent you
 * control triggers, that ability triggers an additional time.
 * Whenever you cast a colorless spell, copy it. You may choose new targets for the copy.
 * (A copy of a permanent spell becomes a token.)
 *
 * The doubler is [AdditionalSourceTriggers] over a colorless filter. The doubler matches the
 * trigger's *source* wherever it is, so a colorless spell's "when you cast this spell" trigger
 * (source on the stack) is doubled as well as a colorless permanent's; the controller check is
 * built into the doubler. `excludeSelf = true` is the "another" — Echoes' own copy trigger is not
 * doubled by itself.
 */
private val colorless = GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless)

val EchoesOfEternity = card("Echoes of Eternity") {
    manaCost = "{3}{C}{C}{C}"
    colorIdentity = ""
    typeLine = "Kindred Enchantment — Eldrazi"
    oracleText = "If a triggered ability of a colorless spell you control or another colorless permanent " +
        "you control triggers, that ability triggers an additional time.\n" +
        "Whenever you cast a colorless spell, copy it. You may choose new targets for the copy. " +
        "(A copy of a permanent spell becomes a token.)"

    staticAbility {
        ability = AdditionalSourceTriggers(
            sourceFilter = colorless,
            excludeSelf = true,
            description = "If a triggered ability of a colorless spell you control or another colorless " +
                "permanent you control triggers, that ability triggers an additional time",
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(colorless)
        effect = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Isis"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b28b9bee-d726-407b-9e5b-2231384df81e.jpg?1783911308"
    }
}
