package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Postmortem Professor — Secrets of Strixhaven #93
 * {1}{B} · Creature — Zombie Warlock · 2/2
 *
 * This creature can't block.
 * Whenever this creature attacks, each opponent loses 1 life and you gain 1 life.
 * {1}{B}, Exile an instant or sorcery card from your graveyard:
 *   Return this card from your graveyard to the battlefield.
 *
 * The recursion ability is activated from the graveyard (`activateFromZone = GRAVEYARD`). Its
 * activation cost combines the mana cost with exiling one instant or sorcery card from the
 * graveyard ([Costs.ExileFromGraveyard]), and it returns the card to the battlefield (untapped)
 * via [Effects.PutOntoBattlefieldFromGraveyard], whose graveyard guard keeps it from moving a card
 * that left the graveyard in response.
 */
val PostmortemProfessor = card("Postmortem Professor") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Warlock"
    power = 2
    toughness = 2
    oracleText = "This creature can't block.\n" +
        "Whenever this creature attacks, each opponent loses 1 life and you gain 1 life.\n" +
        "{1}{B}, Exile an instant or sorcery card from your graveyard: Return this card from your graveyard to the battlefield."

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "Whenever this creature attacks, each opponent loses 1 life and you gain 1 life."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.ExileFromGraveyard(1, GameObjectFilter.InstantOrSorcery),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        description = "{1}{B}, Exile an instant or sorcery card from your graveyard: Return this card from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Nino Vecia"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/174f5d7e-5d36-4d13-96bf-9b12cd644716.jpg?1775937558"
    }
}
