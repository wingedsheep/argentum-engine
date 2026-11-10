package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Accursed Witch // Infectious Curse (Shadows over Innistrad #97)
 * {3}{B} · Creature — Human Shaman 4/2 // Enchantment — Aura Curse
 *
 * Front — Accursed Witch
 *   Spells your opponents cast that target this creature cost {1} less to cast.
 *   When this creature dies, return it to the battlefield transformed under your control attached
 *   to target opponent.
 *
 * Back — Infectious Curse
 *   Enchant player
 *   Spells you cast that target enchanted player cost {1} less to cast.
 *   At the beginning of enchanted player's upkeep, that player loses 1 life and you gain 1 life.
 *
 * Both discounts are a caster-scoped `ModifySpellCost` with a source-relative
 * [CostReductionSource.FixedIfAnyTargetMatches] — Strong Back's shape — so the discount is earned
 * once by any chosen target (the rulings: Aura spells and spells with additional targets count,
 * abilities don't), and the legal-action enumeration already prices it before targets are chosen.
 * "This creature" is the Witch itself (`sourceItself()`); "enchanted player" is the player the
 * Curse is attached to (`attachedToBySource()`, which reads the Curse's attachment and so matches
 * a player id as readily as a permanent's).
 *
 * The return mirrors Radiant Grace: a player host keeps the returned Aura under the trigger's
 * controller, and the face swaps while the card is still in the graveyard.
 */
private val AccursedWitchFront = card("Accursed Witch") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Shaman"
    power = 4
    toughness = 2
    oracleText = "Spells your opponents cast that target this creature cost {1} less to cast.\n" +
        "When this creature dies, return it to the battlefield transformed under your control " +
        "attached to target opponent."

    // Spells your opponents cast that target this creature cost {1} less to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(1, GameObjectFilter.Any.sourceItself())
            )
        )
    }

    // When this creature dies, return it to the battlefield transformed under your control
    // attached to target opponent.
    triggeredAbility {
        trigger = Triggers.self.dies()
        val cursed = target(Targets.Opponent)
        effect = Effects.ReturnSelfToBattlefieldAttached(target = cursed, transformed = true)
        description = "When this creature dies, return it to the battlefield transformed under " +
            "your control attached to target opponent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8093f8b0-5d50-48ca-b616-e92535a47138.jpg?1783937786"
        ruling(
            "2016-04-08",
            "If an effect causes Accursed Witch to transform on the battlefield, rather than leaving " +
                "it and returning transformed, then Infectious Curse doesn't become attached to any " +
                "player and is put into your graveyard. This won't cause abilities that trigger " +
                "when a creature leaves the battlefield to trigger."
        )
    }
}

private val InfectiousCurse = card("Infectious Curse") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n" +
        "Spells you cast that target enchanted player cost {1} less to cast.\n" +
        "At the beginning of enchanted player's upkeep, that player loses 1 life and you gain 1 life."

    auraTarget = Targets.Player

    // Spells you cast that target enchanted player cost {1} less to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(1, GameObjectFilter.Any.attachedToBySource())
            )
        )
    }

    // At the beginning of enchanted player's upkeep, that player loses 1 life and you gain 1 life.
    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EnchantedPlayer)) then
            Effects.GainLife(1)
        description = "At the beginning of enchanted player's upkeep, that player loses 1 life " +
            "and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/back/8/0/8093f8b0-5d50-48ca-b616-e92535a47138.jpg?1783937786"
    }
}

val AccursedWitch: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = AccursedWitchFront,
    backFace = InfectiousCurse,
)
