package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragonlord Kolaghan
 * {4}{B}{R}
 * Legendary Creature — Elder Dragon
 * 6/5
 *
 * Flying, haste
 * Other creatures you control have haste.
 * Whenever an opponent casts a creature or planeswalker spell with the same name as a card in
 * their graveyard, that player loses 10 life.
 *
 * "Their graveyard" is the caster's: a cast trigger's spell filter is evaluated with the caster as
 * the evaluating player, so `sharingNameWithCardInYour(Zone.GRAVEYARD)` compares
 * against the casting opponent's graveyard, never ours. The cast event fires once costs are paid,
 * so a delve spell that exiled every same-named card doesn't trigger (DTK ruling), and a
 * face-down spell has no name and never matches. "That player" is the caster
 * ([Player.TriggeringPlayer]), not a target.
 */
val DragonlordKolaghan = card("Dragonlord Kolaghan") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 6
    toughness = 5
    oracleText = "Flying, haste\n" +
        "Other creatures you control have haste.\n" +
        "Whenever an opponent casts a creature or planeswalker spell with the same name as a card in " +
        "their graveyard, that player loses 10 life."

    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.AllCreaturesYouControl.other())
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(
            GameObjectFilter.CreatureOrPlaneswalker.sharingNameWithCardInYour(Zone.GRAVEYARD)
        )
        effect = Effects.LoseLife(10, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent casts a creature or planeswalker spell with the same name as a " +
            "card in their graveyard, that player loses 10 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "218"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cbf8933-32a1-4892-9bdc-89464ed9ce1b.jpg?1783938573"
        ruling("2015-02-25", "Casting a spell face down won’t cause Dragonlord Kolaghan’s last ability to trigger. A spell with no name can’t have the same name as any other card.")
        ruling("2015-02-25", "If an opponent casts a creature spell with delve and exiles all cards from their graveyard with the same name to help pay its cost, Dragonlord Kolaghan’s ability won’t trigger.")
    }
}
