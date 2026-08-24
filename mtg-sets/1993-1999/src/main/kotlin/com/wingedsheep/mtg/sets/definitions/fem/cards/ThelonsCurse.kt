package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thelon's Curse
 * {G}{G}
 * Enchantment
 * Blue creatures don't untap during their controllers' untap steps.
 * At the beginning of each player's upkeep, that player may choose any number of tapped blue
 * creatures they control and pay {U} for each creature chosen this way. If the player does, untap
 * those creatures.
 *
 * Magnetic Mountain's card, at a hostile price: the ransom is {U} per creature rather than {4}, so
 * it is the *blue* player's own colour that buys their creatures back. The selection cap counts
 * total available mana rather than blue specifically, so a player who over-selects is simply not
 * offered the payment — the gate checks affordability before prompting.
 */
val ThelonsCurse = card("Thelon's Curse") {
    manaCost = "{G}{G}"
    colorIdentity = "GU"
    typeLine = "Enchantment"
    oracleText = "Blue creatures don't untap during their controllers' untap steps.\n" +
        "At the beginning of each player's upkeep, that player may choose any number of tapped " +
        "blue creatures they control and pay {U} for each creature chosen this way. If the player " +
        "does, untap those creatures."

    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.DOESNT_UNTAP.name,
            filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.BLUE))
        )
    }

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(
                        player = Player.TriggeringPlayer,
                        filter = GameObjectFilter.Creature.withColor(Color.BLUE).tapped()
                    ),
                    storeAs = "eligible"
                ),
                SelectFromCollectionEffect(
                    from = "eligible",
                    selection = SelectionMode.ChooseAnyNumber,
                    chooser = Chooser.TriggeringPlayer,
                    storeSelected = "chosen",
                    useTargetingUI = true,
                    prompt = "Choose any number of tapped blue creatures to untap (pay {U} for each)",
                    restrictions = listOf(
                        SelectionRestriction.MaxAffordablePayment(
                            manaPerSelected = 1,
                            payer = Player.TriggeringPlayer
                        )
                    )
                ),
                ConditionalOnCollectionEffect(
                    collection = "chosen",
                    ifNotEmpty = GatedEffect(
                        gate = Gate.MayPay(
                            Effects.PayDynamicMana(
                                amount = DynamicAmount.VariableReference("chosen_count"),
                                payer = Player.TriggeringPlayer,
                                color = Color.BLUE,
                            )
                        ),
                        decisionMaker = EffectTarget.PlayerRef(Player.TriggeringPlayer),
                        then = ForEachInCollectionEffect(
                            collection = "chosen",
                            effect = Effects.Untap(EffectTarget.Self)
                        ),
                        descriptionOverride = "Pay {U} for each chosen creature? If you do, untap them."
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b868846-cc3c-4756-a5dd-2335bb380567.jpg?1783947884"
    }
}
