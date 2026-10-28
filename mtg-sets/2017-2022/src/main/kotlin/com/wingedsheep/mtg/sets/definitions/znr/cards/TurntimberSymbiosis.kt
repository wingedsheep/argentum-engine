package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntryCounter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Turntimber Symbiosis {4}{G}{G}{G} // Turntimber, Serpentine Wood — Zendikar Rising #215
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Turntimber Symbiosis: "Look at the top seven cards of your library. You may put a
 * creature card from among them onto the battlefield. If that card has mana value 3 or less, it
 * enters with three additional +1/+1 counters on it. Put the rest on the bottom of your library in
 * a random order."
 * Back — Turntimber, Serpentine Wood: "As this land enters, you may pay 3 life. If you don't, it
 * enters tapped. {T}: Add {G}."
 *
 * The chosen creature rides a single move onto the battlefield with an [EntryCounter], so the three
 * counters are part of the entry event (Hardened Scales and friends see them). The counter count is
 * read before the move, off the card's mana value *in the library* — the ruling says to ignore
 * replacement effects (Glasspool Mimic) that would change its mana value once it has entered — and
 * a mana value above 3 yields zero counters, which the move drops.
 */
private val TurntimberSymbiosisFront = card("Turntimber Symbiosis") {
    manaCost = "{4}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top seven cards of your library. You may put a creature card from among " +
        "them onto the battlefield. If that card has mana value 3 or less, it enters with three " +
        "additional +1/+1 counters on it. Put the rest on the bottom of your library in a random order."

    spell {
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(7), Player.You))
            val (chosen, rest) = chooseUpToSplit(
                1,
                from = top,
                filter = GameObjectFilter.Creature,
                showAllCards = true,
                prompt = "You may put a creature card onto the battlefield (mana value 3 or less: " +
                    "it enters with three additional +1/+1 counters)",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put on the bottom",
            )
            ifNotEmpty(chosen) {
                run(
                    Effects.Move(
                        chosen.asTarget,
                        Zone.BATTLEFIELD,
                        fromZone = Zone.LIBRARY,
                        entryCounters = listOf(
                            EntryCounter(
                                CounterType.PLUS_ONE_PLUS_ONE,
                                count = DynamicAmounts.conditional(
                                    Conditions.CompareAmounts(
                                        DynamicAmounts.manaValueOf(chosen),
                                        ComparisonOperator.LTE,
                                        3,
                                    ),
                                    ifTrue = 3,
                                    ifFalse = 0,
                                ),
                            )
                        ),
                    )
                )
            }
            move(
                rest,
                CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random,
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "215"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61bd69ea-1e9e-46b0-b1a1-ed7fdbe3deb6.jpg?1783929333"
        ruling("2020-09-25", "Use the mana value of the creature card as it exists in your library to determine whether it receives +1/+1 counters, ignoring any replacement effects (such as that of Glasspool Mimic) that will change its mana value once it's on the battlefield.")
        ruling("2020-09-25", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
    }
}

private val TurntimberSerpentineWoodBack = card("Turntimber, Serpentine Wood") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {G}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "215"
        artist = "Randy Vargas"
        flavorText = "The trees here twist around spikes of mana, feeding off the flowing power.\n—Jori En, expedition leader"
        imageUri = "https://cards.scryfall.io/normal/back/6/1/61bd69ea-1e9e-46b0-b1a1-ed7fdbe3deb6.jpg?1783929333"
    }
}

val TurntimberSymbiosis: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = TurntimberSymbiosisFront,
    backFace = TurntimberSerpentineWoodBack,
)
