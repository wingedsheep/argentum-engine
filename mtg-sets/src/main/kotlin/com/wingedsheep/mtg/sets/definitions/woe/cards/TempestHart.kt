package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.SpellCastEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tempest Hart // Scan the Clouds
 * {3}{G}
 * Creature — Elemental Elk
 * 3/4
 *
 * Trample
 * Whenever you cast a spell with mana value 5 or greater, put a +1/+1 counter on this creature.
 *
 * Adventure: Scan the Clouds — {1}{U}, Instant — Adventure
 * Draw two cards, then discard two cards.
 *
 * The cast trigger is the usual "cast a big spell" payoff shape: a [SpellCastEvent] whose
 * `spellFilter` carries the mana-value threshold, so the check happens against the spell *on the
 * stack*. That matters for {X} spells — per the WOE ruling, the value chosen for X counts toward
 * mana value, and the spell already has its X locked in by the time the trigger sees it.
 * `TriggerBinding.ANY` because the trigger cares about the spell, not about which permanent the
 * event names; the payoff points back at the Hart via [EffectTarget.Self].
 *
 * The Adventure is a plain loot: draw then discard, in that order — the discard sees the two freshly
 * drawn cards, and with a hand of fewer than two cards afterwards you simply discard what you have.
 */
val TempestHart = card("Tempest Hart") {
    manaCost = "{3}{G}"
    colorIdentity = "GU"
    typeLine = "Creature — Elemental Elk"
    power = 3
    toughness = 4
    oracleText = "Trample\n" +
        "Whenever you cast a spell with mana value 5 or greater, put a +1/+1 counter on this creature."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = TriggerSpec(
            event = SpellCastEvent(
                spellFilter = GameObjectFilter.Any.manaValueAtLeast(5),
                player = Player.You,
            ),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    adventure("Scan the Clouds") {
        manaCost = "{1}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Draw two cards, then discard two cards. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.DrawCards(2).then(Effects.Discard(2))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Aldo Domínguez"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/559bacc8-facc-4d93-90b5-8ac21d3246f5.jpg?1783915060"

        ruling(
            "2023-09-01",
            "If a spell has {X} in its mana cost, use the value chosen for that X to determine the " +
                "mana value of that spell."
        )
    }
}
