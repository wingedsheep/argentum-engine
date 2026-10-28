package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CopiedAbilities
import com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nicol Bolas, Dragon-God
 * {U}{B}{B}{B}{R}
 * Legendary Planeswalker — Bolas
 * Starting Loyalty: 4
 *
 * Nicol Bolas has all loyalty abilities of all other planeswalkers on the battlefield.
 * +1: You draw a card. Each opponent exiles a card from their hand or a permanent they control.
 * −3: Destroy target creature or planeswalker.
 * −8: Each opponent who doesn't control a legendary creature or planeswalker loses the game.
 *
 * The borrowed abilities are copies whose source is Bolas (CR 113.7), so they pay loyalty off
 * Bolas and share his one-loyalty-ability-per-turn limit; Bolas never copies his own.
 *
 * The +1's choices are made opponent by opponent in turn order, each picking a card in hand or a
 * permanent they control, and every chosen card is then exiled at once (2024-01-12 ruling) —
 * `forEachPlayerCollecting` gathers the picks, a single `exile` moves them.
 */
val NicolBolasDragonGod = card("Nicol Bolas, Dragon-God") {
    manaCost = "{U}{B}{B}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Planeswalker — Bolas"
    startingLoyalty = 4
    oracleText = "Nicol Bolas has all loyalty abilities of all other planeswalkers on the battlefield.\n" +
        "+1: You draw a card. Each opponent exiles a card from their hand or a permanent they control.\n" +
        "−3: Destroy target creature or planeswalker.\n" +
        "−8: Each opponent who doesn't control a legendary creature or planeswalker loses the game."

    staticAbility {
        ability = GainActivatedAbilitiesOfPermanents(
            grantedTo = GroupFilter.source(),
            sourceFilter = GameObjectFilter.Planeswalker,
            abilities = CopiedAbilities.LOYALTY
        )
    }

    // +1: You draw a card. Each opponent exiles a card from their hand or a permanent they control.
    loyaltyAbility(+1) {
        description = "You draw a card. Each opponent exiles a card from their hand or a permanent they control."
        effect = Effects.DrawCards(1) then Effects.Pipeline {
            val (chosen) = forEachPlayerCollecting(Player.EachOpponent) {
                val pool = gather(CardSource.FromMultipleZones(listOf(Zone.HAND, Zone.BATTLEFIELD), Player.You))
                listOf(
                    chooseExactly(
                        1,
                        from = pool,
                        chooser = Chooser.Controller,
                        prompt = "Exile a card from your hand or a permanent you control"
                    )
                )
            }
            exile(chosen)
        }
    }

    // −3: Destroy target creature or planeswalker.
    loyaltyAbility(-3) {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t)
    }

    // −8: Each opponent who doesn't control a legendary creature or planeswalker loses the game.
    loyaltyAbility(-8) {
        description = "Each opponent who doesn't control a legendary creature or planeswalker loses the game."
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.If(
                condition = Conditions.YouControl(GameObjectFilter.CreatureOrPlaneswalker.legendary(), negate = true),
                then = Effects.LoseGame()
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "207"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98b68dea-a7be-4f99-8a50-4c8cf0e0f7a9.jpg?1783933392"

        ruling("2024-01-12", "Nicol Bolas doesn't remove loyalty abilities from the other planeswalkers.")
        ruling("2024-01-12", "Nicol Bolas doesn't gain any static or triggered abilities of other planeswalkers or any activated abilities they may have that aren't loyalty abilities.")
        ruling("2024-01-12", "While Nicol Bolas may end up with an astonishing number of loyalty abilities, you can still activate only one loyalty ability of Nicol Bolas, Dragon-God during each of your turns.")
        ruling("2024-01-12", "If a loyalty ability of a planeswalker references the card it's printed on by name, treat Nicol Bolas's instance of that ability as though it referenced Nicol Bolas, Dragon-God by name instead. For instance, if Ral Zarek is on the battlefield, Nicol Bolas deals the damage if you activate the second ability he gained from Ral.")
        ruling("2024-01-12", "After you draw a card as Nicol Bolas's first loyalty ability resolves, the next opponent in turn order (or, if it's somehow an opponent's turn, that opponent) chooses a permanent they control or chooses a card in hand without revealing that card. Then each other opponent in turn order does the same, knowing the choices made before them. Finally, all of the chosen permanents and cards are exiled simultaneously.")
    }
}
