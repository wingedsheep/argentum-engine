package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Evendo Brushrazer — Edge of Eternities Commander #10 (canonical printing)
 * {2}{R} · Creature — Insect Warrior · 2/2
 *
 * Whenever you sacrifice a nontoken permanent, exile the top card of your library.
 * During your turn, as long as you've sacrificed a nontoken permanent this turn, you may play
 * cards exiled with this creature.
 * {T}, Sacrifice a land: Add {R}{R}.
 *
 * The trigger fires once per nontoken permanent sacrificed (Savra's per-permanent template) and
 * exiles face up, linked to this creature (CR 607). The play permission is Rogue Class's
 * `MayPlayCardsFromExile` over its own linked pile, gated on your turn and on the per-turn
 * nontoken-sacrifice record — turn history, so it holds for the rest of the turn.
 */
val EvendoBrushrazer = card("Evendo Brushrazer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Insect Warrior"
    oracleText = "Whenever you sacrifice a nontoken permanent, exile the top card of your library.\n" +
        "During your turn, as long as you've sacrificed a nontoken permanent this turn, you may play " +
        "cards exiled with this creature.\n" +
        "{T}, Sacrifice a land: Add {R}{R}."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Permanent.nontoken())
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, player = Player.You))
            move(top, CardDestination.ToZone(Zone.EXILE, Player.You), linkToSource = true)
        }
        description = "Whenever you sacrifice a nontoken permanent, exile the top card of your library."
    }

    staticAbility {
        ability = MayPlayCardsFromExile(
            filter = GameObjectFilter.Any.exiledWithSource(),
            condition = Conditions.All(Conditions.IsYourTurn, Conditions.YouSacrificedNontokenPermanentsThisTurn()),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Land))
        effect = Effects.AddMana(Color.RED, 2)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Igor Grechanyi"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b9001a4-953c-4a32-b51a-c6bec7e74f9e.jpg?1783906065"
        ruling("2025-07-25", "You pay all costs and follow all timing rules for cards played with the permission granted by Evendo Brushrazer's second ability. For example, if an exiled card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
