package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Professor Onyx — Strixhaven: School of Mages #83 (canonical printing)
 * {4}{B}{B} · Legendary Planeswalker — Liliana · Loyalty 5
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, each opponent loses 2 life
 * and you gain 2 life.
 * +1: You lose 1 life. Look at the top three cards of your library. Put one of them into your
 * hand and the rest into your graveyard.
 * −3: Each opponent sacrifices a creature with the greatest power among creatures that player
 * controls.
 * −8: Each opponent may discard a card. If they don't, they lose 3 life. Repeat this process six
 * more times.
 *
 * The magecraft gain is a fixed 2 (not "life lost this way"), so it is a loss then a gain rather
 * than a drain. The −3 is the Extract a Confession edict: `hasGreatestPower()` scopes to each
 * opponent's own creatures, and `Player.EachOpponent` has every opponent choose in turn order
 * before the creatures are sacrificed together. The −8 is the Ob Nixilis, the Adversary punisher
 * ([Effects.PayOrSuffer] with a discard, inside [Effects.ForEachPlayer] over each opponent),
 * wrapped in [Effects.Repeat] seven times — the process plus six repeats.
 */
val ProfessorOnyx = card("Professor Onyx") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 5
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, each opponent " +
        "loses 2 life and you gain 2 life.\n" +
        "+1: You lose 1 life. Look at the top three cards of your library. Put one of them into " +
        "your hand and the rest into your graveyard.\n" +
        "−3: Each opponent sacrifices a creature with the greatest power among creatures that " +
        "player controls.\n" +
        "−8: Each opponent may discard a card. If they don't, they lose 3 life. Repeat this " +
        "process six more times."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(2)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, each " +
            "opponent loses 2 life and you gain 2 life."
    }

    loyaltyAbility(+1) {
        effect = Effects.LoseLife(1, EffectTarget.Controller) then
            Patterns.Library.lookAtTopAndKeep(count = 3, keepCount = 1)
        description = "You lose 1 life. Look at the top three cards of your library. Put one of " +
            "them into your hand and the rest into your graveyard."
    }

    loyaltyAbility(-3) {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature.hasGreatestPower(),
            target = EffectTarget.PlayerRef(Player.EachOpponent)
        )
        description = "Each opponent sacrifices a creature with the greatest power among creatures " +
            "that player controls."
    }

    loyaltyAbility(-8) {
        effect = Effects.Repeat(
            amount = DynamicAmounts.fixed(7),
            body = Effects.ForEachPlayer(
                players = Player.EachOpponent,
                effect = Effects.PayOrSuffer(
                    cost = Costs.pay.Discard(count = 1),
                    suffer = Effects.LoseLife(3, target = EffectTarget.Controller),
                    player = EffectTarget.Controller
                )
            )
        )
        description = "Each opponent may discard a card. If they don't, they lose 3 life. Repeat " +
            "this process six more times."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "83"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/013eeb99-1b66-4fba-ad96-78deee901ea4.jpg?1783927362"
        ruling("2021-04-16", "For Professor Onyx's second loyalty ability, if an opponent has multiple creatures with the greatest power, that player choose which one to sacrifice. The next opponent in turn order chooses which creature they are sacrificing, then each other opponent in turn order does the same, knowing the choices made before them. Then all chosen creatures are sacrificed at the same time.")
        ruling("2021-04-16", "For Professor Onyx's last loyalty ability, \"this process\" is the following: First, the next opponent in turn order may choose a card in hand without revealing it, then each other opponent in turn order does the same. Then all the chosen cards are revealed and discarded at the same time and each opponent who didn't discard a card (whether they chose not to or had an empty hand) loses 3 life. This all will happen seven times, for a maximum loss of 21 life per opponent.")
    }
}
