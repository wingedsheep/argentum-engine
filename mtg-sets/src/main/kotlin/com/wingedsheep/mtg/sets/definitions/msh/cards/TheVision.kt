package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Vision — Marvel Super Heroes #255
 * {4} · Legendary Artifact Creature — Robot Hero · 2/5 · Rare
 *
 * Flying, vigilance
 * Whenever you cast a noncreature spell, choose one that hasn't been chosen this turn —
 * • Solar Beam — The Vision gains double strike until end of turn.
 * • Density Control — The Vision gains indestructible until end of turn.
 * • Technopathy — Draw a card.
 *
 * Implementation notes:
 * - The trigger is [Triggers.youCastSpell] filtered to [GameObjectFilter.Noncreature]; it fires
 *   on cast, so the modal choice is made (and resolves) before the spell itself resolves.
 * - "Choose one that hasn't been chosen this turn" is
 *   [ModalEffect.chooseOneNotYetChosenThisTurn], the Breeches, Eager Pillager primitive: the
 *   engine remembers which modes *this* Vision has chosen during the current turn (per-source,
 *   cleared each cleanup step) and never offers an already-chosen mode again. After all three
 *   have been chosen in a turn the ability has no legal mode and resolves with no effect; the
 *   memory is per object, so two Visions track their chosen modes separately.
 * - Each mode's `description` is the button text, so it carries the printed ability name.
 * - Both grants target [EffectTarget.Self] with [Duration.EndOfTurn] — the ability names the
 *   source, it doesn't target.
 */
val TheVision = card("The Vision") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Robot Hero"
    power = 2
    toughness = 5
    oracleText = "Flying, vigilance\n" +
        "Whenever you cast a noncreature spell, choose one that hasn't been chosen this turn —\n" +
        "• Solar Beam — The Vision gains double strike until end of turn.\n" +
        "• Density Control — The Vision gains indestructible until end of turn.\n" +
        "• Technopathy — Draw a card."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.youCastSpell(GameObjectFilter.Noncreature)
        effect = ModalEffect.chooseOneNotYetChosenThisTurn(
            // • Solar Beam — The Vision gains double strike until end of turn.
            Mode.noTarget(
                Effects.GrantKeyword(
                    keyword = Keyword.DOUBLE_STRIKE,
                    target = EffectTarget.Self,
                    duration = Duration.EndOfTurn
                ),
                "Solar Beam — The Vision gains double strike until end of turn"
            ),
            // • Density Control — The Vision gains indestructible until end of turn.
            Mode.noTarget(
                Effects.GrantKeyword(
                    keyword = Keyword.INDESTRUCTIBLE,
                    target = EffectTarget.Self,
                    duration = Duration.EndOfTurn
                ),
                "Density Control — The Vision gains indestructible until end of turn"
            ),
            // • Technopathy — Draw a card.
            Mode.noTarget(
                Effects.DrawCards(1),
                "Technopathy — Draw a card"
            )
        )
        description = "Whenever you cast a noncreature spell, choose one that hasn't been chosen " +
            "this turn — Solar Beam, Density Control, or Technopathy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "255"
        artist = "Carissa Susilo"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2961cf20-33c8-4e66-9d0f-6daca8ea7880.jpg?1783902887"
    }
}
