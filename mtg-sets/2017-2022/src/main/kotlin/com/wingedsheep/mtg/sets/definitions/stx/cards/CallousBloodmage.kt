package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Callous Bloodmage — Strixhaven: School of Mages #66 (canonical printing)
 * {2}{B} · Creature — Vampire Warlock · 2/1
 *
 * When this creature enters, choose one —
 * • Create a 1/1 black and green Pest creature token with "When this token dies, you gain 1 life."
 * • You draw a card and you lose 1 life.
 * • Exile target player's graveyard.
 *
 * A modal ETB ([ModalEffect.chooseOne]); the Pest is the predefined STX token
 * ([Effects.CreatePest]) and the graveyard-exile mode is the Kutzil's Flanker gather-and-exile
 * pipeline over the targeted player's graveyard.
 */
val CallousBloodmage = card("Callous Bloodmage") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Warlock"
    oracleText = "When this creature enters, choose one —\n" +
        "• Create a 1/1 black and green Pest creature token with \"When this token dies, you gain 1 life.\"\n" +
        "• You draw a card and you lose 1 life.\n" +
        "• Exile target player's graveyard."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.CreatePest(),
                "Create a 1/1 black and green Pest creature token with \"When this token dies, you gain 1 life.\""
            ),
            Mode.noTarget(
                Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller),
                "You draw a card and you lose 1 life"
            ),
            mode("Exile target player's graveyard") {
                val player = target(Targets.Player)
                effect = Effects.Pipeline {
                    val targetGraveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
                    exile(targetGraveyard, player.asPlayer)
                }
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Jokubas Uogintas"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81358736-65cf-44d8-819e-d0268d14cc76.jpg?1783927369"
    }
}
