package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Disruptor Flute
 * {2}
 * Artifact
 * Flash
 * As this artifact enters, choose a card name.
 * Spells with the chosen name cost {3} more to cast.
 * Activated abilities of sources with the chosen name can't be activated unless they're mana abilities.
 *
 * Modeling notes:
 * - The as-enters name choice is [EntersWithChoice]`(ChoiceType.CARD_NAME, CardNamePool.ANY)`, stored
 *   durably under `ChoiceSlot.CARD_NAME` (same shape as Sorcerous Spyglass, minus the hand look).
 * - The tax is symmetric — every caster pays it — so [SpellCostTarget.AnyCaster] over the bare
 *   chosen-name predicate ([GameObjectFilter.namedFromChosenComponent]).
 * - The ability lock is [PreventActivatedAbilities]`(nonManaAbilitiesOnly = true, anyZone = true)` over
 *   the same name — "sources" reaches cards in every zone, so cycling and channel are locked too.
 */
val DisruptorFlute = card("Disruptor Flute") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Flash\n" +
        "As this artifact enters, choose a card name.\n" +
        "Spells with the chosen name cost {3} more to cast.\n" +
        "Activated abilities of sources with the chosen name can't be activated unless they're mana abilities."

    keywords(Keyword.FLASH)

    // As this artifact enters, choose a card name.
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.ANY,
        )
    )

    // Spells with the chosen name cost {3} more to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.AnyCaster(GameObjectFilter.Any.namedFromChosenComponent()),
            modification = CostModification.IncreaseGeneric(3),
        )
    }

    // Activated abilities of sources with the chosen name (in any zone) can't be activated unless mana abilities.
    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Any.namedFromChosenComponent(),
            nonManaAbilitiesOnly = true,
            anyZone = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Xavier Ribeiro"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cad8671-4761-4014-a8a3-af45627e6e79.jpg?1783911243"
    }
}
