package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * One Last Job
 * {2}{W}
 * Sorcery
 *
 * Spree (Choose one or more additional costs.)
 * + {2} — Return target creature card from your graveyard to the battlefield.
 * + {1} — Return target Mount or Vehicle card from your graveyard to the battlefield.
 * + {1} — Return target Aura or Equipment card from your graveyard to the battlefield
 *         attached to a creature you control.
 *
 * Spree is a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`, per-mode
 * `additionalManaCost`, and `allowRepeat = false` (CR 702.166 / 700.2). Every mode targets a
 * card in your graveyard; chosen modes always resolve in printed order (so a creature returned
 * by mode 1 can host an Aura/Equipment returned by mode 3).
 *
 *  - Modes 1 & 2 return the targeted card to the battlefield with [Effects.PutOntoBattlefield]
 *    (the card is owned by you, so it enters under your control). Mode 2's target filter admits
 *    a card with the Mount or Vehicle subtype, which need not be a creature card in the
 *    graveyard.
 *  - Mode 3 returns the targeted Aura/Equipment via
 *    [Effects.PutOntoBattlefieldAttachedToChosen], which puts it onto the battlefield attached
 *    to a creature you control chosen as the spell resolves (the host is NOT a target, per the
 *    ruling). The effect works for both Auras and Equipment and enforces legal attachment: if
 *    no creature you control can host it, an Equipment enters unattached while an Aura can't
 *    return (Rule 303.4g) — matching the One Last Job rulings.
 */
val OneLastJob = card("One Last Job") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {2} — Return target creature card from your graveyard to the battlefield.\n" +
        "+ {1} — Return target Mount or Vehicle card from your graveyard to the battlefield.\n" +
        "+ {1} — Return target Aura or Equipment card from your graveyard to the battlefield " +
        "attached to a creature you control."

    spell {
        effect = ModalEffect(
            modes = listOf(
                // + {2} — Return target creature card from your graveyard to the battlefield.
                Mode(
                    effect = Effects.PutOntoBattlefield(EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(
                        TargetObject(
                            filter = TargetFilter(
                                GameObjectFilter.Creature.ownedByYou(),
                                zone = Zone.GRAVEYARD
                            )
                        )
                    ),
                    description = "+ {2} — Return target creature card from your graveyard to the battlefield.",
                    additionalManaCost = "{2}"
                ),
                // + {1} — Return target Mount or Vehicle card from your graveyard to the battlefield.
                Mode(
                    effect = Effects.PutOntoBattlefield(EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(
                        TargetObject(
                            filter = TargetFilter(
                                GameObjectFilter.Any.withAnySubtype("Mount", "Vehicle").ownedByYou(),
                                zone = Zone.GRAVEYARD
                            )
                        )
                    ),
                    description = "+ {1} — Return target Mount or Vehicle card from your graveyard to the battlefield.",
                    additionalManaCost = "{1}"
                ),
                // + {1} — Return target Aura or Equipment card from your graveyard to the
                //         battlefield attached to a creature you control.
                Mode(
                    effect = Effects.PutOntoBattlefieldAttachedToChosen(
                        target = EffectTarget.ContextTarget(0),
                        hostFilter = GameObjectFilter.Creature.youControl()
                    ),
                    targetRequirements = listOf(
                        TargetObject(
                            filter = TargetFilter(
                                GameObjectFilter.Any.withAnySubtype("Aura", "Equipment").ownedByYou(),
                                zone = Zone.GRAVEYARD
                            )
                        )
                    ),
                    description = "+ {1} — Return target Aura or Equipment card from your graveyard " +
                        "to the battlefield attached to a creature you control.",
                    additionalManaCost = "{1}"
                )
            ),
            chooseCount = 3,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Caroline Gariba"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71bfbfae-e7eb-4f80-81c6-9ab6a1bbd39d.jpg?1712860542"

        ruling("2024-04-12", "The creature that the Aura or Equipment card returned by the third mode will enter attached to isn't a target of that mode and is chosen as One Last Job resolves.")
        ruling("2024-04-12", "The Aura or Equipment must be able to be legally attached to the creature you choose. You can't return an Aura card to the battlefield if there's nothing to enchant. However, if you control no creatures, you could return an Equipment card to the battlefield unattached to anything.")
        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "No matter which modes you choose, you always follow the instructions in the order they are written.")
    }
}
