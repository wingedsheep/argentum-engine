package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ZoneChangeCause
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Psychic Purge
 * {U}
 * Sorcery
 *
 * Psychic Purge deals 1 damage to any target.
 * When a spell or ability an opponent controls causes you to discard this card, that player loses
 * 5 life.
 *
 * - The discard clause is a self-bound discard trigger narrowed to an opponent's spell or ability
 *   ([ZoneChangeCause.DiscardedByOpponentEffect]); "that player" is the causing controller,
 *   [Player.ControllerOfCause]. It triggers from the graveyard it was discarded to, and a cost or
 *   the cleanup-step hand-size discard never fires it.
 */
val PsychicPurge = card("Psychic Purge") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Psychic Purge deals 1 damage to any target.\n" +
        "When a spell or ability an opponent controls causes you to discard this card, that player " +
        "loses 5 life."

    spell {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(1, any)
    }

    triggeredAbility {
        trigger = Triggers.self.isDiscarded(cause = ZoneChangeCause.DiscardedByOpponentEffect)
        effect = Effects.LoseLife(5, EffectTarget.PlayerRef(Player.ControllerOfCause))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1103d4d-b50a-4e2c-b18a-a181bc819881.jpg?1783948073"
    }
}
