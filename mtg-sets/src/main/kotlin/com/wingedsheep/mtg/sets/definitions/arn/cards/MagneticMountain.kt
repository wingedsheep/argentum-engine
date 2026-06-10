package com.wingedsheep.mtg.sets.definitions.arn.cards

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
 * Magnetic Mountain
 * {1}{R}{R}
 * Enchantment
 *
 * Blue creatures don't untap during their controllers' untap steps.
 * At the beginning of each player's upkeep, that player may choose any number of tapped blue
 * creatures they control and pay {4} for each creature chosen this way. If the player does, untap
 * those creatures.
 */
val MagneticMountain = card("Magnetic Mountain") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Blue creatures don't untap during their controllers' untap steps.\n" +
        "At the beginning of each player's upkeep, that player may choose any number of tapped " +
        "blue creatures they control and pay {4} for each creature chosen this way. If the player " +
        "does, untap those creatures."

    // "Blue creatures don't untap during their controllers' untap steps." — a continuous untap-step
    // restriction over every blue creature on the battlefield (CR 502.3), modeled the same way as
    // Choke / Juntu Stakes via the DOESNT_UNTAP keyword.
    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.DOESNT_UNTAP.name,
            filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.BLUE))
        )
    }

    // "At the beginning of each player's upkeep, that player may choose any number of tapped blue
    // creatures they control and pay {4} for each creature chosen this way. If the player does,
    // untap those creatures." — resolution-time choose-any-number + per-creature scaled payment.
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = Effects.Composite(
            listOf(
                // The upkeep player's tapped blue creatures are the eligible pool.
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(
                        player = Player.TriggeringPlayer,
                        filter = GameObjectFilter.Creature.withColor(Color.BLUE).tapped()
                    ),
                    storeAs = "eligible"
                ),
                // The upkeep player chooses any number of them on the battlefield (0 = decline).
                // MaxAffordablePayment caps the choice at what they can actually pay {4} each for,
                // so an over-selection can never silently forfeit the whole untap; with no mana
                // available the prompt is skipped entirely.
                SelectFromCollectionEffect(
                    from = "eligible",
                    selection = SelectionMode.ChooseAnyNumber,
                    chooser = Chooser.TriggeringPlayer,
                    storeSelected = "chosen",
                    useTargetingUI = true,
                    prompt = "Choose any number of tapped blue creatures to untap (pay {4} for each)",
                    restrictions = listOf(
                        SelectionRestriction.MaxAffordablePayment(
                            manaPerSelected = 4,
                            payer = Player.TriggeringPlayer
                        )
                    )
                ),
                // Only prompt for payment when at least one creature was chosen — so a player with no
                // tapped blue creatures (or who declines) is never asked to "pay {0}".
                ConditionalOnCollectionEffect(
                    collection = "chosen",
                    ifNotEmpty = GatedEffect(
                        gate = Gate.MayPay(
                            Effects.PayDynamicMana(
                                amount = DynamicAmount.Multiply(
                                    DynamicAmount.VariableReference("chosen_count"),
                                    4
                                ),
                                payer = Player.TriggeringPlayer
                            )
                        ),
                        decisionMaker = EffectTarget.PlayerRef(Player.TriggeringPlayer),
                        then = ForEachInCollectionEffect(
                            collection = "chosen",
                            effect = Effects.Untap(EffectTarget.Self)
                        ),
                        descriptionOverride = "Pay {4} for each chosen creature? If you do, untap them."
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95fde48b-e40a-4183-b324-1ec276dde015.jpg?1562922858"
    }
}
