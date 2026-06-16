package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Smuggler's Surprise
 * {G}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {2} — Mill four cards. You may put up to two creature and/or land cards from among the
 *         milled cards into your hand.
 * + {4}{G} — You may put up to two creature cards from your hand onto the battlefield.
 * + {1} — Creatures you control with power 4 or greater gain hexproof and indestructible
 *         until end of turn.
 *
 * Spree is a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`, per-mode
 * `additionalManaCost`, and `allowRepeat = false` (CR 700.2 / OTJ release notes). None of the
 * modes target. Modes always resolve in printed order.
 *
 * Mode 1 is the standard mill → gather → select-up-to-two → move-to-hand pipeline (see
 * Cache Grab), scoped to creature/land cards from among the milled cards. Mode 2 gathers
 * creature cards from hand and puts up to two onto the battlefield (see Ghalta). Mode 3
 * grants hexproof + indestructible to your power-4-or-greater creatures via a single
 * group iteration (see Selfless Safewright).
 */
val SmugglersSurprise = card("Smuggler's Surprise") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {2} — Mill four cards. You may put up to two creature and/or land cards from " +
        "among the milled cards into your hand.\n" +
        "+ {4}{G} — You may put up to two creature cards from your hand onto the battlefield.\n" +
        "+ {1} — Creatures you control with power 4 or greater gain hexproof and " +
        "indestructible until end of turn."

    spell {
        effect = ModalEffect(
            modes = listOf(
                // + {2} — Mill four; put up to two creature/land cards from milled into hand.
                Mode(
                    effect = Effects.Composite(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                                storeAs = "smuggler_milled"
                            ),
                            MoveCollectionEffect(
                                from = "smuggler_milled",
                                destination = CardDestination.ToZone(Zone.GRAVEYARD)
                            ),
                            SelectFromCollectionEffect(
                                from = "smuggler_milled",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                                filter = GameObjectFilter.CreatureOrLand,
                                storeSelected = "smuggler_to_hand",
                                showAllCards = true,
                                prompt = "You may put up to two creature and/or land cards into your hand",
                                selectedLabel = "Put in hand",
                                remainderLabel = "Leave in graveyard"
                            ),
                            MoveCollectionEffect(
                                from = "smuggler_to_hand",
                                destination = CardDestination.ToZone(Zone.HAND)
                            )
                        )
                    ),
                    description = "+ {2} — Mill four cards. You may put up to two creature and/or " +
                        "land cards from among the milled cards into your hand.",
                    additionalManaCost = "{2}"
                ),
                // + {4}{G} — Put up to two creature cards from your hand onto the battlefield.
                Mode(
                    effect = Effects.Composite(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature),
                                storeAs = "smuggler_hand_candidates"
                            ),
                            SelectFromCollectionEffect(
                                from = "smuggler_hand_candidates",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                                storeSelected = "smuggler_to_battlefield",
                                prompt = "You may put up to two creature cards onto the battlefield"
                            ),
                            MoveCollectionEffect(
                                from = "smuggler_to_battlefield",
                                destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
                            )
                        )
                    ),
                    description = "+ {4}{G} — You may put up to two creature cards from your hand " +
                        "onto the battlefield.",
                    additionalManaCost = "{4}{G}"
                ),
                // + {1} — Your power-4-or-greater creatures gain hexproof and indestructible.
                Mode(
                    effect = Effects.ForEachInGroup(
                        filter = GroupFilter(
                            baseFilter = GameObjectFilter.Creature.youControl().powerAtLeast(4)
                        ),
                        effect = Effects.Composite(
                            listOf(
                                Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self, Duration.EndOfTurn),
                                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self, Duration.EndOfTurn)
                            )
                        )
                    ),
                    description = "+ {1} — Creatures you control with power 4 or greater gain " +
                        "hexproof and indestructible until end of turn.",
                    additionalManaCost = "{1}"
                )
            ),
            chooseCount = 3,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7fbb489-e2b5-4278-8162-86802cf124d8.jpg?1712860610"

        ruling("2024-04-12", "The effect of Smuggler's Surprise's last mode affects only creatures you control at the time it resolves. Creatures you begin to control later in the turn and noncreature permanents that become creatures later in the turn won't get hexproof or indestructible. However, if you chose Smuggler's Surprise's second mode, any creatures you put onto the battlefield that way will gain hexproof and indestructible until end of turn if their power is 4 or greater.")
        ruling("2024-04-12", "No matter which modes you choose, you always follow the instructions in the order they are written.")
        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
    }
}
