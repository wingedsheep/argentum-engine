package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Soul Transfer — Kamigawa: Neon Dynasty #122 (canonical printing)
 * {1}{B}{B} · Sorcery
 *
 * Choose one. If you control an artifact and an enchantment as you cast this spell, you may choose
 * both instead.
 * • Exile target creature or planeswalker.
 * • Return target creature or planeswalker card from your graveyard to your hand.
 *
 * The Molten Collapse shape: `minChooseCount = 1` keeps "choose one" mandatory, and the cap is a
 * cast-time `dynamicChooseCount` — 2 when you control an artifact and an enchantment, otherwise 1.
 * The check is two independent existence checks, so a single artifact enchantment satisfies it, and
 * because it is evaluated as the spell is cast, losing the artifact or enchantment in response
 * doesn't undo a two-mode choice (both 2022-02-18 rulings).
 */
val SoulTransfer = card("Soul Transfer") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one. If you control an artifact and an enchantment as you cast this spell, " +
        "you may choose both instead.\n" +
        "• Exile target creature or planeswalker.\n" +
        "• Return target creature or planeswalker card from your graveyard to your hand."

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.All(
                    Conditions.YouControl(GameObjectFilter.Artifact),
                    Conditions.YouControl(GameObjectFilter.Enchantment),
                ),
                ifTrue = 2,
                ifFalse = 1
            )
        ) {
            mode("Exile target creature or planeswalker") {
                val victim = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.Exile(victim)
            }
            mode("Return target creature or planeswalker card from your graveyard to your hand") {
                val card = target(
                    TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.ownedByYou(), zone = Zone.GRAVEYARD)
                )
                effect = Effects.ReturnToHand(card)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Lorenzo Mastroianni"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e5aba90-c8c7-4f06-b7b3-1b4758d2791a.jpg?1783923876"
        ruling("2022-02-18", "The artifact and the enchantment may be the same permanent.")
        ruling(
            "2022-02-18",
            "Once you cast Soul Transfer and choose both modes, it doesn't matter what happens to the " +
                "artifact or enchantment you control in response. Soul Transfer will still have both modes chosen."
        )
    }
}
