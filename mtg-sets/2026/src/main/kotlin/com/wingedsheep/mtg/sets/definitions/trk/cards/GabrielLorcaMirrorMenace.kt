package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gabriel Lorca, Mirror Menace
 * {4}{B}
 * Legendary Creature — Human Rogue Officer
 * 5/4
 *
 * Menace
 * Whenever you face a dilemma, you may pay {B}. If you do, return this card from your graveyard to
 * your hand. (You face a dilemma as you choose one or more modes for a spell or ability.)
 *
 * "This card from your graveyard" means the ability functions only from the graveyard
 * (`triggerZone = GRAVEYARD`), where the general non-battlefield event pass sees the
 * [Triggers.you] dilemma event. Paying while the card has already left the graveyard does nothing.
 */
val GabrielLorcaMirrorMenace = card("Gabriel Lorca, Mirror Menace") {
    manaCost = "{4}{B}"
    typeLine = "Legendary Creature — Human Rogue Officer"
    colorIdentity = "B"
    oracleText = "Menace\nWhenever you face a dilemma, you may pay {B}. If you do, return this card from your " +
        "graveyard to your hand. (You face a dilemma as you choose one or more modes for a spell or ability.)"
    power = 5
    toughness = 4
    keywords(Keyword.MENACE)
    triggeredAbility {
        trigger = Triggers.you.facesADilemma()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.MayPay(
            cost = ManaCost.parse("{B}"),
            then = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD),
        )
        description = "Whenever you face a dilemma, you may pay {B}. If you do, return Gabriel Lorca, " +
            "Mirror Menace from your graveyard to your hand."
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Kevin Glint"
        flavorText = "\"The Federation is a social experiment doomed to failure. Childish idealism.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a98d0f01-8e5e-4ff1-88e5-684b8f4c015c.jpg?1793100690"
    }
}
