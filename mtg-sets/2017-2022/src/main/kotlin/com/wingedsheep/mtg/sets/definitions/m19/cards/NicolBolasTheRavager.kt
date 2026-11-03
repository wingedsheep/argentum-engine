package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nicol Bolas, the Ravager // Nicol Bolas, the Arisen — Core Set 2019 #218
 * {1}{U}{B}{R} · Legendary Creature — Elder Dragon 4/4 // Legendary Planeswalker — Bolas (loyalty 7)
 *
 * Front:
 *   Flying
 *   When Nicol Bolas enters, each opponent discards a card.
 *   {4}{U}{B}{R}: Exile Nicol Bolas, then return him to the battlefield transformed under his
 *   owner's control. Activate only as a sorcery.
 * Back:
 *   +2: Draw two cards.
 *   −3: Nicol Bolas deals 10 damage to target creature or planeswalker.
 *   −4: Put target creature or planeswalker card from a graveyard onto the battlefield under your
 *       control.
 *   −12: Exile all but the bottom card of target player's library.
 *
 * Modeling notes:
 *  - The flip is the Magic Origins / Huatli shape: a sorcery-speed activated ability into
 *    [Effects.ExileAndReturnTransformed], so the planeswalker returns as a new object with its
 *    printed loyalty 7.
 *  - −4 is Reanimate's move from any graveyard with the controller overridden to you, widened to
 *    creature or planeswalker cards.
 *  - −12 is Jace, Reality Sculptor's "all but the bottom card" gather, aimed at the target player:
 *    the top (library size − 1) cards, clamped at zero, exiled face up.
 */
private val NicolBolasTheRavagerFront = card("Nicol Bolas, the Ravager") {
    manaCost = "{1}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When Nicol Bolas enters, each opponent discards a card.\n" +
        "{4}{U}{B}{R}: Exile Nicol Bolas, then return him to the battlefield transformed under his " +
        "owner's control. Activate only as a sorcery."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.eachOpponentDiscards(1)
        description = "When Nicol Bolas enters, each opponent discards a card."
    }

    activatedAbility {
        cost = Costs.Mana("{4}{U}{B}{R}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.ExileAndReturnTransformed(EffectTarget.Self)
        description = "Exile Nicol Bolas, then return him to the battlefield transformed under his " +
            "owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "218"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b215968-93a6-4278-ac61-4e3e8c3c3943.jpg?1783934524"

        ruling("2018-07-13", "When Nicol Bolas's enters-the-battlefield triggered ability resolves, first the next opponent in turn order (or, if it's an opponent's turn, that opponent) chooses a card in their hand without revealing it, then each other opponent in turn order does the same. Then all the chosen cards are discarded at the same time.")
        ruling("2018-07-13", "If Nicol Bolas leaves the battlefield after his creature face's activated ability has been activated but before it resolves, the card remains in whichever zone it moves to. It won't be exiled or return transformed.")
        ruling("2018-07-13", "If another permanent that isn't a double-faced card (such as Mirror Image) becomes a copy of Nicol Bolas, the Ravager, activating the ability to exile it and return it transformed will only exile it. It won't return to the battlefield at all.")
        ruling("2018-07-13", "The mana value of a double-faced card is the mana value of its front face, even while it's on the battlefield with its back face up. For example, the mana value of Nicol Bolas, the Arisen is 4.")
        ruling("2018-07-13", "In some rare cases, a spell or ability may cause Nicol Bolas to transform while it's a creature on the battlefield. If this happens, the resulting planeswalker won't have any loyalty counters on it and will subsequently be put into its owner's graveyard.")
    }
}

private val NicolBolasTheArisen = card("Nicol Bolas, the Arisen") {
    manaCost = ""
    colorIdentity = "UBR"
    colorIndicator = "UBR"
    typeLine = "Legendary Planeswalker — Bolas"
    startingLoyalty = 7
    oracleText = "+2: Draw two cards.\n" +
        "−3: Nicol Bolas deals 10 damage to target creature or planeswalker.\n" +
        "−4: Put target creature or planeswalker card from a graveyard onto the battlefield under " +
        "your control.\n" +
        "−12: Exile all but the bottom card of target player's library."

    loyaltyAbility(+2) {
        effect = Effects.DrawCards(2)
        description = "Draw two cards."
    }

    loyaltyAbility(-3) {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(10, t)
        description = "Nicol Bolas deals 10 damage to target creature or planeswalker."
    }

    loyaltyAbility(-4) {
        val card = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = Zone.GRAVEYARD))
        effect = Effects.Move(
            card,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.GRAVEYARD,
        )
        description = "Put target creature or planeswalker card from a graveyard onto the " +
            "battlefield under your control."
    }

    loyaltyAbility(-12) {
        target(Targets.Player)
        effect = Effects.Pipeline {
            val arisenExiled = gather(
                CardSource.TopOfLibrary(
                    DynamicAmounts.nonNegative(
                        DynamicAmounts.count(Player.TargetPlayer, Zone.LIBRARY) - 1
                    ),
                    Player.TargetPlayer,
                )
            )
            exile(arisenExiled)
        }
        description = "Exile all but the bottom card of target player's library."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "218"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/back/7/b/7b215968-93a6-4278-ac61-4e3e8c3c3943.jpg?1783934524"

        ruling("2018-07-13", "Nicol Bolas, the Arisen has a color indicator on its typeline. This color indicator means that it's a blue, black, and red permanent.")
        ruling("2018-07-13", "In a multiplayer game, if a player leaves the game, all cards that player owns leave as well. If you leave the game, any permanents you control from Nicol Bolas's third loyalty ability that came from other players' graveyards are exiled.")
    }
}

val NicolBolasTheRavager: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = NicolBolasTheRavagerFront,
    backFace = NicolBolasTheArisen,
)
