package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reaper's Talisman (AFR #117) — {B} Artifact — Equipment (uncommon)
 *
 * Whenever equipped creature attacks, it gains deathtouch until end of turn.
 * Whenever equipped creature attacks alone, defending player loses 2 life and you gain 2 life.
 * Equip {2}
 *
 * Both are the Equipment's own triggers (`Triggers.attached.attacks`). "It" is the attacking
 * creature — [EffectTarget.TriggeringEntity], which the attachment trigger binds to the equipped
 * creature, so the deathtouch stays with that creature even if the Talisman moves in response.
 * "Defending player" resolves through the triggering attacker (the Equipment itself isn't
 * attacking), i.e. the player that creature is attacking.
 */
val ReapersTalisman = card("Reaper's Talisman") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature attacks, it gains deathtouch until end of turn.\n" +
        "Whenever equipped creature attacks alone, defending player loses 2 life and you gain 2 life.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    // Whenever equipped creature attacks, it gains deathtouch until end of turn.
    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.TriggeringEntity)
    }

    // Whenever equipped creature attacks alone, defending player loses 2 life and you gain 2 life.
    triggeredAbility {
        trigger = Triggers.attached.attacks(setOf(AttackPredicate.Alone))
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.DefendingPlayer)) then
            Effects.GainLife(2)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Olena Richards"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93b68168-e77d-4745-9393-726ef9fb72ec.jpg?1783926490"
    }
}
