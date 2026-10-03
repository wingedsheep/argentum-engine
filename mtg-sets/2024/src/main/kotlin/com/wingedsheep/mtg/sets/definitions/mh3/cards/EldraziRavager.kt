package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eldrazi Ravager
 * {5}{C}
 * Creature — Eldrazi
 * 6/6
 *
 * Annihilator 1
 * Sacrifice two Eldrazi: Return this card from your graveyard to your hand.
 * Cycling {2}
 *
 * Modeling notes:
 *  - Annihilator is display-only [KeywordAbility.Numeric] vocabulary, so it is lowered here as the
 *    attack trigger it abbreviates — an edict on the defending player over any permanent (same
 *    shape as Nulldrifter / Artisan of Kozilek).
 *  - "two **Eldrazi**" is a bare tribal noun: any permanent with the subtype, not only creatures.
 */
val EldraziRavager = card("Eldrazi Ravager") {
    manaCost = "{5}{C}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 6
    toughness = 6
    oracleText = "Annihilator 1 (Whenever this creature attacks, defending player sacrifices a " +
        "permanent of their choice.)\n" +
        "Sacrifice two Eldrazi: Return this card from your graveyard to your hand.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    keywordAbility(KeywordAbility.annihilator(1))
    keywordAbility(KeywordAbility.cycling("{2}"))

    // Annihilator 1 — the lowering of the display-only keyword ability above.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Sacrifice(
            GameObjectFilter.Permanent,
            1,
            EffectTarget.PlayerRef(Player.DefendingPlayer)
        )
        description = "Annihilator 1"
    }

    activatedAbility {
        cost = Costs.SacrificeMultiple(2, GameObjectFilter.Permanent.withSubtype("Eldrazi"))
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Martin de Diego Sádaba"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0ce97bb8-b0ee-4473-8c26-a3ec91abec97.jpg?1783911308"

        ruling(
            "2024-06-07",
            "Annihilator abilities trigger and resolve during the declare attackers step. The " +
                "defending player sacrifices the required number of permanents of their choice " +
                "before they declare blockers. Any creatures sacrificed this way won't be able " +
                "to block."
        )
        ruling(
            "2024-06-07",
            "If a creature with annihilator is attacking a planeswalker, and the defending " +
                "player chooses to sacrifice that planeswalker, the attacking creature continues " +
                "to attack. It may be blocked. If it isn't blocked, it simply won't deal combat " +
                "damage to anything."
        )
    }
}
