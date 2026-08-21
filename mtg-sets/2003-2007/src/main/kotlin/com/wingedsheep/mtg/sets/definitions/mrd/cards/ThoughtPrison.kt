package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Thought Prison — Mirrodin #261
 * {5} · Artifact · Uncommon
 *
 * Imprint — When this artifact enters, you may have target player reveal their hand. If you do,
 * choose a nonland card from it and exile that card.
 * Whenever a player casts a spell that shares a color or mana value with the exiled card, this
 * artifact deals 2 damage to that player.
 *
 * Modelling notes:
 * - The imprint and the payoff are a *linked* pair (CR 607): `linkToSource = true` files the exiled
 *   card in this artifact's own pile, and the cast trigger reads that pile back through
 *   [EntityReference.LinkedExiledCard] — "the exiled card", not "a card in exile".
 * - "Shares a color **or** mana value" is two independent predicates over the same reference, OR-ed
 *   at the filter level. Neither knows about the other: `sharingColorWith` was already there for
 *   any entity reference, and `sharingManaValueWith` is its mana-value sibling. Note that colorless
 *   is not a color, so two colorless cards share no *color* — but they do share a mana value if the
 *   numbers match, which is how a colorless imprint still bites.
 * - Declining the imprint (or the exiled card later leaving exile) makes the reference resolve to
 *   nothing, both predicates false, and the trigger silent — the correct fail-closed reading.
 * - The trigger fires on *any* player casting, including this artifact's controller, and the damage
 *   goes to the caster (`Player.TriggeringPlayer`).
 * - "You may have target player reveal their hand. If you do, …" gates the whole reveal-choose-exile
 *   chain, so the reveal sits *inside* the optional half rather than being hoisted out of it. The
 *   controller of Thought Prison — not the revealing player — chooses which nonland card is exiled.
 */
val ThoughtPrison = card("Thought Prison") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Imprint — When this artifact enters, you may have target player reveal their " +
        "hand. If you do, choose a nonland card from it and exile that card.\n" +
        "Whenever a player casts a spell that shares a color or mana value with the exiled card, " +
        "this artifact deals 2 damage to that player."

    // "Imprint — When this artifact enters, you may have target player reveal their hand. If you
    // do, choose a nonland card from it and exile that card."
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val player = target("target player", Targets.Player)
        effect = Patterns.Hand.revealHandAndExileChosen(
            target = player,
            filter = GameObjectFilter.Nonland,
            prompt = "Choose a nonland card to exile with Thought Prison",
            storeChosenAs = "thoughtPrisonImprint",
            revealHand = true,
            linkToSource = true
        )
        description = "Imprint — When this artifact enters, you may have target player reveal " +
            "their hand. If you do, choose a nonland card from it and exile that card."
    }

    // "Whenever a player casts a spell that shares a color or mana value with the exiled card,
    // this artifact deals 2 damage to that player."
    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(
            spellFilter = GameObjectFilter.Any.sharingColorWith(EntityReference.LinkedExiledCard()) or
                GameObjectFilter.Any.sharingManaValueWith(EntityReference.LinkedExiledCard())
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever a player casts a spell that shares a color or mana value with the " +
            "exiled card, this artifact deals 2 damage to that player."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "261"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/409087ef-8232-489c-8b98-b601bb0a47a4.jpg?1783944499"
    }
}
