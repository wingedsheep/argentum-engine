package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gideon's Intervention
 * {2}{W}{W}
 * Enchantment
 * As this enchantment enters, choose a card name.
 * Your opponents can't cast spells with the chosen name.
 * Prevent all damage that would be dealt to you and permanents you control by sources with the
 * chosen name.
 *
 * The name is chosen as it enters ([EntersWithChoice], any card name, as Runed Halo) and stored on
 * the permanent; both statics read it through [GameObjectFilter.namedFromChosenComponent], which the
 * cast-prohibition check and the damage-prevention source filter evaluate with this enchantment as
 * the predicate source.
 */
val GideonsIntervention = card("Gideon's Intervention") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a card name.\n" +
        "Your opponents can't cast spells with the chosen name.\n" +
        "Prevent all damage that would be dealt to you and permanents you control by sources with the chosen name."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.ANY,
        )
    )

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            spellFilter = GameObjectFilter.Any.namedFromChosenComponent(),
        )
    }

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.AnyOf(listOf(Recipient.You, Recipient.PermanentYouControl)),
                source = GameObjectFilter.Any.namedFromChosenComponent(),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e2524fb-ec90-4f8e-b851-f78034edef3f.jpg?1783936539"
    }
}
