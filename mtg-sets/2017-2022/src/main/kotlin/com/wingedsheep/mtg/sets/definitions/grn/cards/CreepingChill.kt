package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Creeping Chill
 * {3}{B}
 * Sorcery
 * Creeping Chill deals 3 damage to each opponent and you gain 3 life.
 * When Creeping Chill is put into your graveyard from your library, you may exile it. If you do,
 * Creeping Chill deals 3 damage to each opponent and you gain 3 life.
 *
 * The library-to-graveyard trigger is Narcomoeba's shape (functions from the graveyard); the
 * "you may exile it. If you do" is Cavalier of Thorns' [Effects.May] over [Effects.IfYouDo], so
 * nothing happens if the card already left the graveyard before the trigger resolves.
 */
val CreepingChill = card("Creeping Chill") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Creeping Chill deals 3 damage to each opponent and you gain 3 life.\n" +
        "When Creeping Chill is put into your graveyard from your library, you may exile it. If you do, " +
        "Creeping Chill deals 3 damage to each opponent and you gain 3 life."

    spell {
        effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(3)
    }

    triggeredAbility {
        trigger = Triggers.self.changesZone(from = Zone.LIBRARY, to = Zone.GRAVEYARD)
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.GRAVEYARD),
                then = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)) then
                    Effects.GainLife(3),
            ),
            descriptionOverride = "You may exile Creeping Chill. If you do, Creeping Chill deals 3 damage " +
                "to each opponent and you gain 3 life.",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5456173-7a08-4b5c-8450-7123375f4a86.jpg?1783934178"
        ruling("2018-10-05", "In a Two-Headed Giant game, Creeping Chill causes the opposing team to lose 6 life and you gain 3 life.")
    }
}
