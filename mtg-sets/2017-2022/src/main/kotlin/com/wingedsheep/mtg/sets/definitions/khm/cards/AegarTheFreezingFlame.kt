package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Aegar, the Freezing Flame — Kaldheim #200 (canonical printing)
 * {1}{U}{R} · Legendary Creature — Giant Wizard 3/3
 *
 * An observer trigger on any source's excess damage to an opponent's creature or planeswalker. The
 * "if a Giant, Wizard, or spell you controlled dealt damage to it this turn" clause is read off the
 * damaged permanent's own record of its damage sources (each frozen as it dealt the damage, the
 * triggering hit included), so it narrows the recipient filter. That record only grows during the
 * turn, so checking it as the trigger fires gives the resolution-time answer too. "A spell" that
 * deals damage is an instant or sorcery, the house reading (Flame Channeler).
 */
val AegarTheFreezingFlame = card("Aegar, the Freezing Flame") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Giant Wizard"
    power = 3
    toughness = 3
    oracleText = "Whenever a creature or planeswalker an opponent controls is dealt excess damage, " +
        "if a Giant, Wizard, or spell you controlled dealt damage to it this turn, draw a card."

    triggeredAbility {
        val giantWizardOrSpell = (
            GameObjectFilter.Any.withSubtype("Giant") or
                GameObjectFilter.Any.withSubtype("Wizard") or
                GameObjectFilter.InstantOrSorcery
            ).youControl()
        trigger = Triggers.a().dealsDamage(
            to = Recipient.Object(
                GameObjectFilter.CreatureOrPlaneswalker.opponentControls()
                    .wasDealtDamageThisTurnBy(giantWizardOrSpell)
            ),
            requireExcess = true,
        )
        effect = Effects.DrawCards(1)
        description = "Whenever a creature or planeswalker an opponent controls is dealt excess damage, " +
            "if a Giant, Wizard, or spell you controlled dealt damage to it this turn, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68011f60-6202-48f4-8255-fb94764e2951.jpg?1783928202"
        ruling("2021-02-05", "A creature has been dealt excess damage if one or more sources deal more damage to it than the minimum amount of damage required to be lethal damage. In most cases, this means damage greater than its toughness, but consider the damage already dealt to it that turn.")
        ruling("2021-02-05", "A planeswalker is dealt excess damage if it's dealt damage greater than its current loyalty.")
        ruling("2021-02-05", "Even 1 damage dealt to a creature from a source with deathtouch is considered lethal damage, so any amount greater than that will cause excess damage to be dealt, even if the total amount of damage isn't greater than the creature's toughness. Note that a source of damage having deathtouch has no effect on damage dealt to planeswalkers.")
        ruling("2021-02-05", "It doesn't matter whether a Giant, Wizard, or spell you control deals the excess damage, only that excess damage was dealt and that one of those three things dealt damage to the creature or planeswalker at some point during the turn. For example, if a 4/4 creature an opponent controls is dealt 2 damage by a spell you control and later that turn is dealt 3 damage by a spell another player controls, Aegar's ability will trigger.")
        ruling("2021-02-05", "If a permanent is both a creature and a planeswalker, the minimum amount of damage to be considered lethal damage is used to determine if excess damage has been dealt. For example, if a 5/5 creature that's also a planeswalker with three loyalty counters on it is dealt 4 damage, it's been dealt 1 excess damage and Aegar's ability may trigger.")
        ruling("2023-04-14", "Aegar's ability won't trigger when a battle is dealt excess damage. It's not his fault. They didn't exist yet.")
    }
}
