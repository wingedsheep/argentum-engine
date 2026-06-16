package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Honest Rutstein
 * {1}{B}{G}
 * Legendary Creature — Human Warlock
 * 3/2
 *
 * When Honest Rutstein enters, return target creature card from your graveyard to your hand.
 * Creature spells you cast cost {1} less to cast.
 */
val HonestRutstein = card("Honest Rutstein") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Warlock"
    power = 3
    toughness = 2
    oracleText = "When Honest Rutstein enters, return target creature card from your graveyard to your hand.\n" +
        "Creature spells you cast cost {1} less to cast."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature", Targets.CreatureCardInYourGraveyard)
        effect = Effects.Move(creature, Zone.HAND)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Javier Charro"
        flavorText = "\"No questions. No refunds.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/5/259ddf66-76af-4857-83c3-c812327a6e23.jpg?1712356107"
    }
}
