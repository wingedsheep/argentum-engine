package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Galvanic Giant // Storm Reading
 * {3}{U}
 * Creature — Giant Wizard
 * 3/3
 *
 * Whenever you cast a spell with mana value 5 or greater, tap target creature an opponent
 * controls and put a stun counter on it.
 *
 * Adventure: Storm Reading — {5}{U}{U}, Instant — Adventure
 * Draw four cards, then discard two cards.
 *
 * The trigger is an ordinary `youCastSpell` with a mana-value floor on the spell filter; the
 * payoff mirrors the WOE tap-and-stun shape (Snaremaster Sprite, Freeze in Place). An
 * already-tapped creature is a legal target and still gets the stun counter.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val GalvanicGiant = card("Galvanic Giant") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Giant Wizard"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast a spell with mana value 5 or greater, tap target creature an " +
        "opponent controls and put a stun counter on it. (If a permanent with a stun counter would " +
        "become untapped, remove one from it instead.)"

    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.manaValueAtLeast(5))
        val t = target(
            "target creature an opponent controls",
            TargetCreature(filter = TargetFilter.Creature.opponentControls())
        )
        effect = Effects.Tap(t) then Effects.AddCounters(Counters.STUN, 1, t)
    }

    adventure("Storm Reading") {
        manaCost = "{5}{U}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Draw four cards, then discard two cards. (Then exile this card. You may cast " +
            "the creature later from exile.)"
        spell {
            effect = Effects.DrawCards(4) then Effects.Discard(2)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60976109-30ad-4f12-99eb-c5ef560fcf1b.jpg?1783915121"
    }
}
