package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Exdeath, Void Warlock // Neo Exdeath, Dimension's End — Final Fantasy #220
 * {1}{B}{G} · Legendary Creature — Spirit Warlock · 3/3 // Legendary Creature — Spirit Avatar · * /3
 *
 * Front — Exdeath, Void Warlock:
 *   When Exdeath enters, you gain 3 life.
 *   At the beginning of your end step, if there are six or more permanent cards in your
 *   graveyard, transform Exdeath.
 *
 * Back — Neo Exdeath, Dimension's End:
 *   Trample
 *   Neo Exdeath's power is equal to the number of permanent cards in your graveyard.
 *
 * The end-step transform is an intervening-"if" (checked both when the trigger would be put on
 * the stack and again on resolution) counting permanent cards in your graveyard via
 * [DynamicAmount.Count] over [GameObjectFilter.Permanent]. The back face's power is a
 * characteristic-defining ability recomputed continuously over the same graveyard count; its
 * printed toughness is a fixed 3, so only `dynamicPower` is set.
 */
private val NeoExdeathDimensionsEnd = card("Neo Exdeath, Dimension's End") {
    manaCost = ""
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Spirit Avatar"
    oracleText = "Trample\nNeo Exdeath's power is equal to the number of permanent cards in your graveyard."
    dynamicPower(
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Permanent),
    )
    toughness = 3

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Jessica Fong"
        flavorText = "\"All memories . . . dimensions . . . existence . . . All that is shall be returned to nothing.\""
        imageUri = "https://cards.scryfall.io/normal/back/1/b/1b4bab87-4000-461d-8b58-d34928fee305.jpg?1782686429"
    }
}

private val ExdeathVoidWarlockFrontFace = card("Exdeath, Void Warlock") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Spirit Warlock"
    oracleText = "When Exdeath enters, you gain 3 life.\n" +
        "At the beginning of your end step, if there are six or more permanent cards in your graveyard, transform Exdeath."
    power = 3
    toughness = 3

    // When Exdeath enters, you gain 3 life.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(3)
    }

    // At the beginning of your end step, if there are six or more permanent cards in your
    // graveyard, transform Exdeath.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Permanent),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(6),
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Jessica Fong"
        flavorText = "\"Mwa-hahahaha! Witness what befalls those who stand against me!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b4bab87-4000-461d-8b58-d34928fee305.jpg?1782686429"
    }
}

val ExdeathVoidWarlock: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ExdeathVoidWarlockFrontFace,
    backFace = NeoExdeathDimensionsEnd,
)
